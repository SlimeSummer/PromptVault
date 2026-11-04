package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.dto.GroupItemOrderRequest;
import com.promptvault.entity.Lora;
import com.promptvault.entity.Tag;
import com.promptvault.entity.TagGroup;
import com.promptvault.entity.TagGroupItem;
import com.promptvault.mapper.LoraMapper;
import com.promptvault.mapper.TagGroupItemMapper;
import com.promptvault.mapper.TagGroupMapper;
import com.promptvault.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TagGroupService {

    public static final List<String> KINDS = List.of("normal", "global_negative");
    public static final List<String> SCOPES = List.of("global", "lora");
    public static final List<String> POLARITIES = List.of("pos", "neg");

    private final TagGroupMapper groupMapper;
    private final TagGroupItemMapper itemMapper;
    private final LoraMapper loraMapper;
    private final TagMapper tagMapper;

    public TagGroupService(TagGroupMapper groupMapper,
                           TagGroupItemMapper itemMapper,
                           LoraMapper loraMapper,
                           TagMapper tagMapper) {
        this.groupMapper = groupMapper;
        this.itemMapper = itemMapper;
        this.loraMapper = loraMapper;
        this.tagMapper = tagMapper;
    }

    public List<TagGroup> list(String scope, Long loraId, String keyword) {
        String kw = keyword == null ? null : keyword.trim();
        String safeScope = null;
        if ("lora".equals(scope)) {
            if (loraId == null) {
                throw new BizException("LoRA 范围需要指定 loraId");
            }
            requireLora(loraId);
            safeScope = "lora";
        } else if ("global".equals(scope)) {
            safeScope = "global";
        }
        return groupMapper.list(safeScope, loraId, kw);
    }

    public Map<String, Object> detail(Long id) {
        TagGroup group = requireGroup(id);
        List<TagGroupItem> items = itemMapper.listByGroup(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("group", group);
        result.put("items", items);
        return result;
    }

    public TagGroup create(TagGroup group) {
        normalize(group);
        String scope = group.getScope();
        Long loraId = group.getLoraId();
        if ("lora".equals(scope)) {
            if (loraId == null) {
                throw new BizException("LoRA 范围需要指定 loraId");
            }
            requireLora(loraId);
        } else {
            group.setLoraId(null);
        }
        group.setSortOrder(groupMapper.maxSortOrder(scope, loraId) + 1);
        groupMapper.insert(group);
        return groupMapper.findById(group.getId());
    }

    public TagGroup update(Long id, TagGroup group) {
        TagGroup existing = requireGroup(id);

        TagGroup target = new TagGroup();
        target.setId(id);

        // 名称留空则沿用原值，避免 NOT NULL 失败
        String name = group.getName() == null ? null : group.getName().trim();
        if (name == null || name.isEmpty()) {
            name = existing.getName();
        }
        if (name.length() > 100) {
            throw new BizException("词组名称过长（最多 100 字符）");
        }
        target.setName(name);

        String kind = group.getKind() == null ? existing.getKind() : group.getKind().trim();
        if (!KINDS.contains(kind)) {
            throw new BizException("未知 kind：" + kind);
        }
        target.setKind(kind);

        String scope = group.getScope() == null ? existing.getScope() : group.getScope().trim();
        if (!SCOPES.contains(scope)) {
            throw new BizException("未知 scope：" + scope);
        }
        target.setScope(scope);

        Long loraId = null;
        if ("lora".equals(scope)) {
            loraId = group.getLoraId() == null ? existing.getLoraId() : group.getLoraId();
            if (loraId == null) {
                throw new BizException("LoRA 范围需要指定 loraId");
            }
            requireLora(loraId);
        }
        target.setLoraId(loraId);
        target.setNote(blankToNull(group.getNote()));

        // 范围 / 归属变化时重新排到新桶的末尾，避免与已有 sort_order 冲突
        boolean relocated = !scope.equals(existing.getScope())
                || (loraId != null && !loraId.equals(existing.getLoraId()));
        target.setSortOrder(relocated
                ? groupMapper.maxSortOrder(scope, loraId) + 1
                : existing.getSortOrder());

        groupMapper.update(target);
        return groupMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        TagGroup existing = requireGroup(id);
        // 阶段 4 workspace 完成后在此加 workspace 引用检查
        if (itemMapper.listByGroup(id).isEmpty()) {
            itemMapper.deleteByGroup(id);
        }
        groupMapper.delete(id);
    }

    /** 复制词组：保留所有条目，名称加"副本" */
    @Transactional
    public TagGroup duplicate(Long id) {
        TagGroup source = requireGroup(id);
        TagGroup copy = new TagGroup();
        copy.setName(source.getName() + " 副本");
        copy.setKind(source.getKind());
        copy.setScope(source.getScope());
        copy.setLoraId(source.getLoraId());
        copy.setNote(source.getNote());
        copy.setSortOrder(groupMapper.maxSortOrder(source.getScope(), source.getLoraId()) + 1);
        groupMapper.insert(copy);

        for (TagGroupItem item : itemMapper.listByGroup(id)) {
            TagGroupItem copied = new TagGroupItem();
            copied.setGroupId(copy.getId());
            copied.setTagId(item.getTagId());
            copied.setPolarity(item.getPolarity());
            copied.setWeight(item.getWeight());
            copied.setSortOrder(item.getSortOrder());
            itemMapper.insert(copied);
        }
        return groupMapper.findById(copy.getId());
    }

    // --- 条目管理 ---

    public TagGroupItem addItem(Long groupId, TagGroupItem item) {
        requireGroup(groupId);
        if (item.getTagId() == null) {
            throw new BizException("请选择要添加的词条");
        }
        Tag tag = tagMapper.findById(item.getTagId());
        if (tag == null) {
            throw new BizException("词条不存在，可能已被删除");
        }
        String polarity = normalizePolarity(item.getPolarity());
        if (itemMapper.countDuplicate(groupId, item.getTagId(), polarity) > 0) {
            throw new BizException("该词条已在" + polarityLabel(polarity) + "列表中");
        }

        TagGroupItem created = new TagGroupItem();
        created.setGroupId(groupId);
        created.setTagId(item.getTagId());
        created.setPolarity(polarity);
        created.setWeight(normalizeWeight(item.getWeight()));
        created.setSortOrder(itemMapper.maxSortOrder(groupId, polarity) + 1);
        itemMapper.insert(created);
        return itemMapper.findById(created.getId());
    }

    public TagGroupItem updateItem(Long groupId, Long itemId, TagGroupItem item) {
        requireGroup(groupId);
        TagGroupItem existing = requireItem(groupId, itemId);

        String polarity = item.getPolarity() == null
                ? existing.getPolarity()
                : normalizePolarity(item.getPolarity());
        int sortOrder = existing.getSortOrder();
        if (!polarity.equals(existing.getPolarity())) {
            if (itemMapper.countDuplicate(groupId, existing.getTagId(), polarity) > 0) {
                throw new BizException("该词条已在" + polarityLabel(polarity) + "列表中");
            }
            sortOrder = itemMapper.maxSortOrder(groupId, polarity) + 1;
        }

        TagGroupItem update = new TagGroupItem();
        update.setId(itemId);
        update.setPolarity(polarity);
        update.setWeight(item.getWeight() == null
                ? existing.getWeight()
                : normalizeWeight(item.getWeight()));
        update.setSortOrder(sortOrder);
        itemMapper.update(update);
        return itemMapper.findById(itemId);
    }

    public void deleteItem(Long groupId, Long itemId) {
        requireGroup(groupId);
        requireItem(groupId, itemId);
        itemMapper.delete(itemId);
    }

    @Transactional
    public void reorder(Long groupId, GroupItemOrderRequest request) {
        requireGroup(groupId);
        if (request == null || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new BizException("排序内容为空");
        }
        int order = 0;
        for (Long itemId : request.getItemIds()) {
            requireItem(groupId, itemId);
            itemMapper.updateSort(itemId, order++);
        }
    }

    // --- 规范化 / 校验 ---

    private void normalize(TagGroup group) {
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new BizException("词组名称不能为空");
        }
        group.setName(group.getName().trim());
        if (group.getName().length() > 100) {
            throw new BizException("词组名称过长（最多 100 字符）");
        }
        String kind = group.getKind() == null ? "normal" : group.getKind().trim();
        if (!KINDS.contains(kind)) {
            throw new BizException("未知 kind：" + kind);
        }
        group.setKind(kind);
        String scope = group.getScope() == null ? "global" : group.getScope().trim();
        if (!SCOPES.contains(scope)) {
            throw new BizException("未知 scope：" + scope);
        }
        group.setScope(scope);
        group.setNote(blankToNull(group.getNote()));
    }

    private String normalizePolarity(String polarity) {
        String value = polarity == null ? "pos" : polarity.trim();
        if (!POLARITIES.contains(value)) {
            throw new BizException("未知正负向：" + value);
        }
        return value;
    }

    private Double normalizeWeight(Double weight) {
        double value = weight == null ? 1.0 : weight;
        if (value < 0.1 || value > 2.0) {
            throw new BizException("权重范围需在 0.1 ~ 2.0 之间");
        }
        return Math.round(value * 100) / 100.0;
    }

    private String polarityLabel(String polarity) {
        return "neg".equals(polarity) ? "负向" : "正向";
    }

    private Lora requireLora(Long loraId) {
        Lora lora = loraMapper.findById(loraId);
        if (lora == null) {
            throw new BizException("LoRA 不存在，可能已被删除");
        }
        return lora;
    }

    private TagGroup requireGroup(Long id) {
        TagGroup group = groupMapper.findById(id);
        if (group == null) {
            throw new BizException("词组不存在，可能已被删除");
        }
        return group;
    }

    private TagGroupItem requireItem(Long groupId, Long itemId) {
        TagGroupItem item = itemMapper.findById(itemId);
        if (item == null || !groupId.equals(item.getGroupId())) {
            throw new BizException("词组条目不存在，可能已被删除");
        }
        return item;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
