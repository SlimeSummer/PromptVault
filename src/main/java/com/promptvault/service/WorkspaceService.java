package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.WorkspaceBlock;
import com.promptvault.dto.WorkspaceItemRequest;
import com.promptvault.dto.WorkspaceOrderRequest;
import com.promptvault.entity.Lora;
import com.promptvault.entity.Preset;
import com.promptvault.entity.PresetItem;
import com.promptvault.entity.Tag;
import com.promptvault.entity.TagGroup;
import com.promptvault.entity.TagGroupItem;
import com.promptvault.entity.Workspace;
import com.promptvault.entity.WorkspaceItem;
import com.promptvault.mapper.LoraMapper;
import com.promptvault.mapper.PresetItemMapper;
import com.promptvault.mapper.PresetMapper;
import com.promptvault.mapper.TagGroupMapper;
import com.promptvault.mapper.TagMapper;
import com.promptvault.mapper.WorkspaceItemMapper;
import com.promptvault.mapper.WorkspaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 编辑区：条目只保存"插入了什么"，渲染时按词条分类动态分区展开。
 * 正向分区完全由 setting.tag.categories 驱动，不写死"九分区"；
 * 负向单独成层，按 全局负面词组 → 预设负向 → 编辑区负向 合并去重。
 */
@Service
public class WorkspaceService {

    private static final String TYPE_TAG = "tag";
    private static final String TYPE_GROUP = "group";
    private static final String POLARITY_POS = "pos";
    private static final String POLARITY_NEG = "neg";
    /** 分类中的 negative 不作为正向分区，负向统一走 polarity */
    private static final String NEGATIVE_CATEGORY = "negative";
    /** 画师分类不进普通分区，单独在画师卡片按 @画师名 输出 */
    private static final String ARTIST_CATEGORY = "artist";
    private static final double MIN_WEIGHT = 0.1;
    private static final double MAX_WEIGHT = 2.0;

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceItemMapper itemMapper;
    private final TagGroupMapper groupMapper;
    private final TagGroupService groupService;
    private final PresetMapper presetMapper;
    private final PresetItemMapper presetItemMapper;
    private final TagMapper tagMapper;
    private final LoraMapper loraMapper;
    private final SettingService settingService;

    public WorkspaceService(WorkspaceMapper workspaceMapper,
                            WorkspaceItemMapper itemMapper,
                            TagGroupMapper groupMapper,
                            TagGroupService groupService,
                            PresetMapper presetMapper,
                            PresetItemMapper presetItemMapper,
                            TagMapper tagMapper,
                            LoraMapper loraMapper,
                            SettingService settingService) {
        this.workspaceMapper = workspaceMapper;
        this.itemMapper = itemMapper;
        this.groupMapper = groupMapper;
        this.groupService = groupService;
        this.presetMapper = presetMapper;
        this.presetItemMapper = presetItemMapper;
        this.tagMapper = tagMapper;
        this.loraMapper = loraMapper;
        this.settingService = settingService;
    }

    public List<Workspace> list() {
        return workspaceMapper.list();
    }

    public Workspace create(Workspace request) {
        Workspace target = new Workspace();
        fill(target, request);
        workspaceMapper.insert(target);
        return workspaceMapper.findById(target.getId());
    }

    public Workspace update(Long id, Workspace request) {
        Workspace existing = requireWorkspace(id);
        Workspace target = new Workspace();
        target.setId(id);
        fill(target, request);

        // 换 LoRA 时，旧 LoRA 专属的词组会跨范围，直接拒绝，避免渲染时静默丢失
        Long newLoraId = target.getLoraId();
        if (existing.getLoraId() != null && !existing.getLoraId().equals(newLoraId)) {
            for (WorkspaceItem item : itemMapper.listByWorkspace(id)) {
                if (!TYPE_GROUP.equals(item.getItemType())) {
                    continue;
                }
                TagGroup group = groupMapper.findById(item.getRefId());
                if (group != null && "lora".equals(group.getScope())
                        && group.getLoraId() != null && !group.getLoraId().equals(newLoraId)) {
                    throw new BizException("编辑区包含旧 LoRA 的词组「" + group.getName() + "」，请先移除后再切换 LoRA");
                }
            }
        }

        workspaceMapper.update(target);
        return workspaceMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        requireWorkspace(id);
        itemMapper.deleteByWorkspace(id);
        workspaceMapper.delete(id);
    }

    // --- 条目管理 ---

    public WorkspaceItem addItem(Long workspaceId, WorkspaceItemRequest request) {
        Workspace workspace = requireWorkspace(workspaceId);
        if (request == null || request.getRefId() == null) {
            throw new BizException("请选择要添加的内容");
        }
        String type = request.getItemType() == null ? "" : request.getItemType().trim();
        String sectionOverride = normalizeSection(request.getSectionOverride());

        WorkspaceItem item = new WorkspaceItem();
        item.setWorkspaceId(workspaceId);
        item.setItemType(type);
        item.setRefId(request.getRefId());
        item.setSectionOverride(sectionOverride);

        if (TYPE_TAG.equals(type)) {
            Tag tag = tagMapper.findById(request.getRefId());
            if (tag == null) {
                throw new BizException("词条不存在，可能已被删除");
            }
            String polarity = normalizePolarity(request.getPolarity());
            if (itemMapper.countDuplicate(workspaceId, TYPE_TAG, tag.getId(), polarity, null) > 0) {
                throw new BizException("该词条已在编辑区" + polarityLabel(polarity) + "中");
            }
            item.setPolarity(polarity);
            item.setWeight(normalizeWeight(request.getWeight()));
        } else if (TYPE_GROUP.equals(type)) {
            TagGroup group = groupMapper.findById(request.getRefId());
            if (group == null) {
                throw new BizException("词组不存在，可能已被删除");
            }
            if ("lora".equals(group.getScope())
                    && group.getLoraId() != null && !group.getLoraId().equals(workspace.getLoraId())) {
                throw new BizException("该词组属于其他 LoRA，当前编辑区无法使用");
            }
            if (itemMapper.countDuplicate(workspaceId, TYPE_GROUP, group.getId(), POLARITY_POS, null) > 0) {
                throw new BizException("该词组已在编辑区中");
            }
            item.setPolarity(POLARITY_POS);
            item.setWeight(1.0);
        } else {
            throw new BizException("未支持的条目类型：" + type);
        }

        item.setSortOrder(itemMapper.maxSortOrder(workspaceId) + 10);
        itemMapper.insert(item);
        return itemMapper.findById(item.getId());
    }

    public WorkspaceItem updateItem(Long workspaceId, Long itemId, WorkspaceItemRequest request) {
        requireWorkspace(workspaceId);
        WorkspaceItem existing = requireItem(workspaceId, itemId);
        if (request == null) {
            return existing;
        }

        WorkspaceItem update = new WorkspaceItem();
        update.setId(itemId);
        update.setPolarity(existing.getPolarity());
        update.setWeight(existing.getWeight());
        update.setSectionOverride(normalizeSection(request.getSectionOverride()));

        if (TYPE_TAG.equals(existing.getItemType())) {
            String polarity = request.getPolarity() == null
                    ? existing.getPolarity()
                    : normalizePolarity(request.getPolarity());
            if (!polarity.equals(existing.getPolarity())
                    && itemMapper.countDuplicate(workspaceId, TYPE_TAG, existing.getRefId(), polarity, itemId) > 0) {
                throw new BizException("该词条已在编辑区" + polarityLabel(polarity) + "中");
            }
            update.setPolarity(polarity);
            update.setWeight(request.getWeight() == null
                    ? existing.getWeight()
                    : normalizeWeight(request.getWeight()));
        }
        // 词组条目只有一个块，权重跟随词组内部条目，分区可手动覆盖
        itemMapper.update(update);
        return itemMapper.findById(itemId);
    }

    public void deleteItem(Long workspaceId, Long itemId) {
        requireWorkspace(workspaceId);
        requireItem(workspaceId, itemId);
        itemMapper.delete(itemId);
    }

    public void reorder(Long workspaceId, WorkspaceOrderRequest request) {
        requireWorkspace(workspaceId);
        if (request == null || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new BizException("排序内容为空");
        }
        int order = 0;
        for (Long itemId : request.getItemIds()) {
            requireItem(workspaceId, itemId);
            itemMapper.updateSort(itemId, order);
            order += 10;
        }
    }

    /**
     * 解绑词组：把递归引用链合并后的条目拆成独立的词条条目，原词组块删除。
     * 已存在的同词条同极性条目跳过。
     */
    @Transactional
    public Map<String, Object> unbind(Long workspaceId, Long itemId) {
        requireWorkspace(workspaceId);
        WorkspaceItem item = requireItem(workspaceId, itemId);
        if (!TYPE_GROUP.equals(item.getItemType())) {
            throw new BizException("只有词组可以解绑");
        }
        TagGroup group = groupMapper.findById(item.getRefId());
        if (group == null) {
            throw new BizException("词组不存在，可能已被删除");
        }

        int added = 0;
        int skipped = 0;
        for (TagGroupItem source : groupService.effectiveItems(group.getId())) {
            if (source.getTagId() == null) {
                skipped++;
                continue;
            }
            String polarity = normalizePolarity(source.getPolarity());
            if (itemMapper.countDuplicate(workspaceId, TYPE_TAG, source.getTagId(), polarity, null) > 0) {
                skipped++;
                continue;
            }
            WorkspaceItem created = new WorkspaceItem();
            created.setWorkspaceId(workspaceId);
            created.setItemType(TYPE_TAG);
            created.setRefId(source.getTagId());
            created.setPolarity(polarity);
            created.setWeight(normalizeWeight(source.getWeight()));
            created.setSectionOverride(POLARITY_POS.equals(polarity) ? item.getSectionOverride() : null);
            created.setSortOrder(itemMapper.maxSortOrder(workspaceId) + 10);
            itemMapper.insert(created);
            added++;
        }
        itemMapper.delete(itemId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupName", group.getName());
        result.put("added", added);
        result.put("skipped", skipped);
        return result;
    }

    // --- 渲染 / 导出 ---

    public Map<String, Object> render(Long id) {
        Workspace workspace = requireWorkspace(id);

        List<CategoryOption> sections = positiveSections();
        List<String> sectionValues = new ArrayList<>();
        Map<String, String> sectionLabels = new LinkedHashMap<>();
        Map<String, List<WorkspaceBlock>> blocksBySection = new LinkedHashMap<>();
        for (CategoryOption option : sections) {
            sectionValues.add(option.getValue());
            sectionLabels.put(option.getValue(), option.getLabel());
            blocksBySection.put(option.getValue(), new ArrayList<>());
        }

        List<Map<String, Object>> negatives = new ArrayList<>();
        Set<String> negativeKeys = new HashSet<>();

        List<Map<String, Object>> artists = new ArrayList<>();
        Map<String, Map<String, Object>> artistIndex = new LinkedHashMap<>();

        // 1. 预设（角色基线）：正向按分类分区，负向进入负向层
        Preset preset = workspace.getPresetId() == null
                ? null
                : presetMapper.findById(workspace.getPresetId());
        if (preset != null) {
            List<PresetItem> presetItems = presetItemMapper.listByPreset(preset.getId());
            Map<String, List<PresetItem>> presetBySection = new LinkedHashMap<>();
            for (PresetItem item : presetItems) {
                if (POLARITY_NEG.equals(item.getPolarity())) {
                    addNegative(negatives, negativeKeys, item.getPrompt(), item.getNameZh(),
                            item.getWeight(), "预设·" + preset.getName(), null);
                } else if (ARTIST_CATEGORY.equals(item.getCategory())) {
                    addArtist(artists, artistIndex, item.getPrompt(), item.getNameZh(),
                            item.getWeight(), "预设·" + preset.getName(), null);
                } else {
                    String section = sectionFor(item.getCategory(), sectionValues);
                    presetBySection.computeIfAbsent(section, k -> new ArrayList<>()).add(item);
                }
            }
            for (Map.Entry<String, List<PresetItem>> entry : presetBySection.entrySet()) {
                WorkspaceBlock block = new WorkspaceBlock();
                block.setKey("preset-" + preset.getId() + "-" + entry.getKey());
                block.setItemType("preset");
                block.setSection(entry.getKey());
                block.setPolarity(POLARITY_POS);
                block.setSourceName("预设·" + preset.getName());
                block.setLocked(true);
                block.setUnbindable(false);
                block.setSortOrder(-100000);
                for (PresetItem item : entry.getValue()) {
                    block.getTags().add(tagMap(item.getTagId(), item.getPrompt(), item.getNameZh(),
                            item.getCategory(), item.getWeight()));
                }
                blocksBySection.get(entry.getKey()).add(block);
            }
        }

        // 2. 全局负面词组（第一层）
        for (TagGroup group : groupMapper.listGlobalNegative()) {
            for (TagGroupItem item : groupService.effectiveItems(group.getId())) {
                if (POLARITY_NEG.equals(item.getPolarity())) {
                    addNegative(negatives, negativeKeys, item.getPrompt(), item.getNameZh(),
                            item.getWeight(), "全局负面·" + group.getName(), null);
                }
            }
        }

        // 3. 编辑区手动添加的标签 / 词组
        for (WorkspaceItem row : itemMapper.listByWorkspace(id)) {
            if (TYPE_TAG.equals(row.getItemType())) {
                renderTagRow(row, sectionValues, blocksBySection, negatives, negativeKeys,
                        artists, artistIndex);
            } else if (TYPE_GROUP.equals(row.getItemType())) {
                renderGroupRow(row, sectionValues, blocksBySection, negatives, negativeKeys,
                        artists, artistIndex);
            }
        }

        // 分区内排序 + 导出文本
        StringBuilder positiveText = new StringBuilder();
        for (CategoryOption option : sections) {
            List<WorkspaceBlock> blocks = blocksBySection.get(option.getValue());
            blocks.sort(Comparator.comparing(WorkspaceBlock::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(WorkspaceBlock::getKey));
            Set<String> sectionKeys = new HashSet<>();
            for (WorkspaceBlock block : blocks) {
                for (Map<String, Object> tag : block.getTags()) {
                    String prompt = text(tag.get("prompt"));
                    if (prompt.isEmpty()) {
                        continue;
                    }
                    String key = prompt.toLowerCase(Locale.ROOT);
                    if (!sectionKeys.add(key)) {
                        continue;
                    }
                    append(positiveText, formatPrompt(prompt, weight(tag.get("weight"))));
                }
            }
        }

        // 画师统一以 @画师名 追加在正向尾部（anima 语法），权重格式 (@kantoku:1.2)
        for (Map<String, Object> artist : artists) {
            append(positiveText, formatPrompt("@" + text(artist.get("prompt")),
                    weight(artist.get("weight"))));
        }

        StringBuilder negativeText = new StringBuilder();
        for (Map<String, Object> item : negatives) {
            append(negativeText, formatPrompt(text(item.get("prompt")), weight(item.get("weight"))));
        }

        List<Map<String, Object>> sectionPayload = new ArrayList<>();
        int positiveCount = 0;
        for (CategoryOption option : sections) {
            List<WorkspaceBlock> blocks = blocksBySection.get(option.getValue());
            int count = blocks.stream().mapToInt(b -> b.getTags().size()).sum();
            positiveCount += count;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("value", option.getValue());
            payload.put("label", option.getLabel());
            payload.put("count", count);
            payload.put("blocks", blocks);
            sectionPayload.add(payload);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspace", workspace);
        result.put("sections", sectionPayload);
        result.put("artists", artists);
        result.put("artistCount", artists.size());
        result.put("negative", negatives);
        result.put("negativeCount", negatives.size());
        result.put("positiveCount", positiveCount);
        result.put("positiveText", positiveText.toString());
        result.put("negativeText", negativeText.toString());
        return result;
    }

    private void renderTagRow(WorkspaceItem row,
                              List<String> sectionValues,
                              Map<String, List<WorkspaceBlock>> blocksBySection,
                              List<Map<String, Object>> negatives,
                              Set<String> negativeKeys,
                              List<Map<String, Object>> artists,
                              Map<String, Map<String, Object>> artistIndex) {
        if (row.getPrompt() == null || row.getPrompt().isBlank()) {
            return; // 词条已被删除
        }
        String polarity = normalizePolarity(row.getPolarity());
        if (POLARITY_NEG.equals(polarity)) {
            // 编辑区手动添加的负向条目可单独移除
            addNegative(negatives, negativeKeys, row.getPrompt(), row.getNameZh(),
                    row.getWeight(), "编辑区", row.getId());
            return;
        }
        if (ARTIST_CATEGORY.equals(row.getCategory())) {
            addArtist(artists, artistIndex, row.getPrompt(), row.getNameZh(),
                    row.getWeight(), "编辑区", row.getId());
            return;
        }
        String section = sectionFor(
                row.getSectionOverride() != null ? row.getSectionOverride() : row.getCategory(),
                sectionValues);
        WorkspaceBlock block = new WorkspaceBlock();
        block.setKey("tag-" + row.getId() + "-" + section);
        block.setItemType(TYPE_TAG);
        block.setWorkspaceItemId(row.getId());
        block.setSection(section);
        block.setSectionOverride(row.getSectionOverride());
        block.setPolarity(POLARITY_POS);
        block.setSourceName("编辑区");
        block.setLocked(false);
        block.setUnbindable(false);
        block.setSortOrder(row.getSortOrder());
        block.getTags().add(tagMap(row.getRefId(), row.getPrompt(), row.getNameZh(),
                row.getCategory(), row.getWeight()));
        blocksBySection.get(section).add(block);
    }

    private void renderGroupRow(WorkspaceItem row,
                                List<String> sectionValues,
                                Map<String, List<WorkspaceBlock>> blocksBySection,
                                List<Map<String, Object>> negatives,
                                Set<String> negativeKeys,
                                List<Map<String, Object>> artists,
                                Map<String, Map<String, Object>> artistIndex) {
        TagGroup group = groupMapper.findById(row.getRefId());
        if (group == null) {
            return; // 词组已被删除
        }
        Map<String, List<TagGroupItem>> grouped = new LinkedHashMap<>();
        for (TagGroupItem item : groupService.effectiveItems(group.getId())) {
            if (POLARITY_NEG.equals(item.getPolarity())) {
                addNegative(negatives, negativeKeys, item.getPrompt(), item.getNameZh(),
                        item.getWeight(), "词组·" + group.getName(), null);
                continue;
            }
            if (ARTIST_CATEGORY.equals(item.getCategory())) {
                addArtist(artists, artistIndex, item.getPrompt(), item.getNameZh(),
                        item.getWeight(), "词组·" + group.getName(), null);
                continue;
            }
            String section = sectionFor(
                    row.getSectionOverride() != null ? row.getSectionOverride() : item.getCategory(),
                    sectionValues);
            grouped.computeIfAbsent(section, k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<TagGroupItem>> entry : grouped.entrySet()) {
            WorkspaceBlock block = new WorkspaceBlock();
            block.setKey("group-" + row.getId() + "-" + entry.getKey());
            block.setItemType(TYPE_GROUP);
            block.setWorkspaceItemId(row.getId());
            block.setGroupId(group.getId());
            block.setGroupName(group.getName());
            block.setSection(entry.getKey());
            block.setSectionOverride(row.getSectionOverride());
            block.setPolarity(POLARITY_POS);
            block.setSourceName("词组·" + group.getName());
            block.setLocked(false);
            block.setUnbindable(true);
            block.setSortOrder(row.getSortOrder());
            for (TagGroupItem item : entry.getValue()) {
                block.getTags().add(tagMap(item.getTagId(), item.getPrompt(), item.getNameZh(),
                        item.getCategory(), item.getWeight()));
            }
            blocksBySection.get(entry.getKey()).add(block);
        }
    }

    // --- 辅助 ---

    private void fill(Workspace target, Workspace request) {
        if (request == null) {
            throw new BizException("编辑区内容不能为空");
        }
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new BizException("编辑区名称不能为空");
        }
        if (name.length() > 100) {
            throw new BizException("编辑区名称过长（最多 100 字符）");
        }
        target.setName(name);
        target.setNote(blankToNull(request.getNote()));

        Long loraId = request.getLoraId();
        if (loraId != null) {
            Lora lora = loraMapper.findById(loraId);
            if (lora == null) {
                throw new BizException("LoRA 不存在，可能已被删除");
            }
        }
        Long presetId = request.getPresetId();
        if (presetId != null) {
            Preset preset = presetMapper.findById(presetId);
            if (preset == null) {
                throw new BizException("预设不存在，可能已被删除");
            }
            if (loraId == null) {
                loraId = preset.getLoraId();
            } else if (!loraId.equals(preset.getLoraId())) {
                throw new BizException("预设不属于所选 LoRA");
            }
        }
        target.setLoraId(loraId);
        target.setPresetId(presetId);
    }

    private List<CategoryOption> positiveSections() {
        List<CategoryOption> result = new ArrayList<>();
        for (CategoryOption option : settingService.getCategories()) {
            if (!NEGATIVE_CATEGORY.equals(option.getValue())
                    && !ARTIST_CATEGORY.equals(option.getValue())) {
                result.add(option);
            }
        }
        if (result.isEmpty()) {
            result.add(new CategoryOption("other", "其他"));
        }
        return result;
    }

    private String sectionFor(String category, List<String> sectionValues) {
        if (category == null || category.isBlank()
                || NEGATIVE_CATEGORY.equals(category)
                || ARTIST_CATEGORY.equals(category)
                || !sectionValues.contains(category)) {
            return sectionValues.contains("other") ? "other" : sectionValues.get(0);
        }
        return category;
    }

    private String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return null;
        }
        String value = section.trim();
        if (NEGATIVE_CATEGORY.equals(value)) {
            throw new BizException("负向内容请直接使用负向 polarity");
        }
        if (ARTIST_CATEGORY.equals(value)) {
            throw new BizException("画师请在画师卡片中选择");
        }
        for (CategoryOption option : positiveSections()) {
            if (value.equals(option.getValue())) {
                return value;
            }
        }
        throw new BizException("未知分区：" + value);
    }

    private String normalizePolarity(String polarity) {
        if (polarity == null || polarity.isBlank()) {
            return POLARITY_POS;
        }
        String value = polarity.trim();
        if (!POLARITY_POS.equals(value) && !POLARITY_NEG.equals(value)) {
            throw new BizException("未知极性：" + value);
        }
        return value;
    }

    private String polarityLabel(String polarity) {
        return POLARITY_NEG.equals(polarity) ? "负向" : "正向";
    }

    private Double normalizeWeight(Double weight) {
        if (weight == null) {
            return 1.0;
        }
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new BizException("权重范围需在 0.1 ~ 2.0 之间");
        }
        return weight;
    }

    private Map<String, Object> tagMap(Long tagId, String prompt, String nameZh,
                                       String category, Double weight) {
        Map<String, Object> tag = new LinkedHashMap<>();
        tag.put("tagId", tagId);
        tag.put("prompt", prompt);
        tag.put("nameZh", nameZh);
        tag.put("category", category);
        tag.put("weight", weight == null ? 1.0 : weight);
        return tag;
    }

    private void addNegative(List<Map<String, Object>> list, Set<String> keys,
                             String prompt, String nameZh, Double weight, String source,
                             Long workspaceItemId) {
        String value = text(prompt);
        if (value.isEmpty()) {
            return;
        }
        if (!keys.add(value.toLowerCase(Locale.ROOT))) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("prompt", value);
        item.put("nameZh", nameZh);
        item.put("weight", weight == null ? 1.0 : weight);
        item.put("source", source);
        item.put("workspaceItemId", workspaceItemId);
        item.put("removable", workspaceItemId != null);
        list.add(item);
    }

    private void addArtist(List<Map<String, Object>> list,
                           Map<String, Map<String, Object>> index,
                           String prompt, String nameZh, Double weight, String source,
                           Long workspaceItemId) {
        String name = text(prompt);
        while (name.startsWith("@")) {
            name = name.substring(1).trim();
        }
        if (name.isEmpty()) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        Map<String, Object> existing = index.get(key);
        if (existing != null) {
            // 同一个画师优先让「编辑区」来源可移除，权重以编辑区为准
            if (existing.get("workspaceItemId") == null && workspaceItemId != null) {
                existing.put("workspaceItemId", workspaceItemId);
                existing.put("weight", weight == null ? 1.0 : weight);
                existing.put("source", source);
                existing.put("removable", true);
            }
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("prompt", name);
        item.put("nameZh", nameZh);
        item.put("weight", weight == null ? 1.0 : weight);
        item.put("source", source);
        item.put("workspaceItemId", workspaceItemId);
        item.put("removable", workspaceItemId != null);
        index.put(key, item);
        list.add(item);
    }

    private void append(StringBuilder builder, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value);
    }

    private String formatPrompt(String prompt, double weight) {
        if (Math.abs(weight - 1.0) < 0.0001) {
            return prompt;
        }
        BigDecimal value = BigDecimal.valueOf(weight).stripTrailingZeros();
        return "(" + prompt + ":" + value.toPlainString() + ")";
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private double weight(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 1.0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Workspace requireWorkspace(Long id) {
        Workspace workspace = workspaceMapper.findById(id);
        if (workspace == null) {
            throw new BizException("编辑区不存在，可能已被删除");
        }
        return workspace;
    }

    private WorkspaceItem requireItem(Long workspaceId, Long itemId) {
        WorkspaceItem item = itemMapper.findById(itemId);
        if (item == null || !workspaceId.equals(item.getWorkspaceId())) {
            throw new BizException("编辑区条目不存在，可能已被删除");
        }
        return item;
    }
}
