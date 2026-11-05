package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.dto.GroupItemOrderRequest;
import com.promptvault.entity.GroupRef;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TagGroupService {

    public static final List<String> KINDS = List.of("normal", "global_negative");
    public static final List<String> SCOPES = List.of("global", "lora");
    public static final List<String> POLARITIES = List.of("pos", "neg");
    /** 环检测最大搜索深度（防止脏数据导致的死循环） */
    private static final int MAX_REF_DEPTH = 10;

    private final TagGroupMapper groupMapper;
    private final TagGroupItemMapper itemMapper;
    private final LoraMapper loraMapper;
    private final TagMapper tagMapper;
    private final SettingService settingService;

    public TagGroupService(TagGroupMapper groupMapper,
                           TagGroupItemMapper itemMapper,
                           LoraMapper loraMapper,
                           TagMapper tagMapper,
                           SettingService settingService) {
        this.groupMapper = groupMapper;
        this.itemMapper = itemMapper;
        this.loraMapper = loraMapper;
        this.tagMapper = tagMapper;
        this.settingService = settingService;
    }

    public List<TagGroup> list(String scope, Long loraId, String keyword, String category) {
        String kw = keyword == null ? null : keyword.trim();
        String cat = category == null || category.isBlank() ? null : category.trim();
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
        List<TagGroup> result = groupMapper.list(safeScope, loraId, cat, kw);
        // 填充被引用次数（列表展示用）
        for (TagGroup g : result) {
            g.setIncomingRefCount(groupMapper.countIncomingRefs(g.getId()));
        }
        return result;
    }

    public List<String> suggest(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return groupMapper.suggest(kw, safeLimit);
    }

    /**
     * 详情：返回组自身 + 自身条目 + 引用列表 + 被引用列表（删除保护用）
     * 以及合并后的 effectiveItems（自身 + 递归所有引用链按深度+优先级去重，自身优先）
     */
    public Map<String, Object> detail(Long id) {
        TagGroup group = requireGroup(id);
        List<TagGroupItem> ownItems = itemMapper.listByGroup(id);
        List<GroupRef> refs = groupMapper.listRefs(id);
        List<GroupRef> incomingRefs = groupMapper.listIncomingRefs(id);
        group.setIncomingRefCount((long) incomingRefs.size());

        // 递归拉引用链的条目：从当前组出发，BFS收集所有可达的组的条目
        // 深度近的优先，同深度按 priority + sort_order
        List<TagGroupItem> refItems = collectRefItemsRecursive(id);

        // 自身条目标记为自有
        for (TagGroupItem item : ownItems) {
            item.setSourceGroupId(id);
            item.setSourceGroupName(group.getName());
        }

        // 计算生效条目：自身 > 递归引用（深度近优先，同深度priority高优先）
        List<TagGroupItem> effectiveItems = computeEffectiveItems(ownItems, refItems);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("group", group);
        result.put("items", ownItems);
        result.put("refs", refs);
        result.put("incomingRefs", incomingRefs);
        result.put("refItems", refItems);
        result.put("effectiveItems", effectiveItems);
        return result;
    }

    /**
     * 供编辑区渲染使用：自身 + 递归引用链合并后的生效条目（已按优先级去重）。
     */
    public List<TagGroupItem> effectiveItems(Long groupId) {
        TagGroup group = requireGroup(groupId);
        List<TagGroupItem> ownItems = itemMapper.listByGroup(groupId);
        for (TagGroupItem item : ownItems) {
            item.setSourceGroupId(groupId);
            item.setSourceGroupName(group.getName());
        }
        List<TagGroupItem> refItems = collectRefItemsRecursive(groupId);
        return computeEffectiveItems(ownItems, refItems);
    }

    /**
     * 递归收集所有引用链的条目：
     * BFS从当前组出发，沿ref_group_id逐层扩展，深度≤MAX_REF_DEPTH。
     * 每层内按 priority → sort_order 排序，深度近的层先入队。
     */
    private List<TagGroupItem> collectRefItemsRecursive(Long groupId) {
        List<TagGroupItem> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(groupId);

        // 每层是一个 List<GroupRef>，表示当前层要遍历的引用
        List<Long> currentLayer = new ArrayList<>();
        for (GroupRef r : groupMapper.listRefs(groupId)) {
            if (visited.add(r.getRefGroupId())) {
                currentLayer.add(r.getRefGroupId());
            }
        }

        int depth = 1;
        while (!currentLayer.isEmpty() && depth <= MAX_REF_DEPTH) {
            // 先把当前层所有组的条目加进结果
            for (Long refGroupId : currentLayer) {
                TagGroup refGroup = groupMapper.findById(refGroupId);
                if (refGroup == null) continue;
                List<TagGroupItem> items = itemMapper.listByGroup(refGroupId);
                for (TagGroupItem item : items) {
                    item.setSourceGroupId(refGroupId);
                    item.setSourceGroupName(refGroup.getName());
                    result.add(item);
                }
            }

            // 再找下一层（当前层所有组引用的组）
            List<Long> nextLayer = new ArrayList<>();
            for (Long refGroupId : currentLayer) {
                for (GroupRef r : groupMapper.listRefs(refGroupId)) {
                    if (visited.add(r.getRefGroupId())) {
                        nextLayer.add(r.getRefGroupId());
                    }
                }
            }
            currentLayer = nextLayer;
            depth++;
        }
        return result;
    }

    /**
     * 合并生效条目：自身优先，引用按 priority 升序（小=高）、sort_order 升序。
     * 同 tagId + 同 polarity 只保留一个，优先级高的胜。
     */
    private List<TagGroupItem> computeEffectiveItems(List<TagGroupItem> ownItems,
                                                      List<TagGroupItem> refItems) {
        // 先放引用（按已排好的顺序），自身后放自然覆盖
        Map<String, TagGroupItem> merged = new LinkedHashMap<>();
        for (TagGroupItem item : refItems) {
            String key = itemKey(item);
            if (!merged.containsKey(key)) {
                merged.put(key, item);
            }
        }
        for (TagGroupItem item : ownItems) {
            String key = itemKey(item);
            // 自身条目总是覆盖引用
            merged.put(key, item);
        }
        return new ArrayList<>(merged.values());
    }

    private String itemKey(TagGroupItem item) {
        return item.getTagId() + "|" + item.getPolarity();
    }

    // --- 组 CRUD ---

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

        String category = group.getCategory() == null || group.getCategory().isBlank()
                ? existing.getCategory()
                : group.getCategory().trim();
        if (!settingService.hasGroupCategory(category)) {
            throw new BizException("未知词组分类：" + category);
        }
        target.setCategory(category);
        target.setNote(blankToNull(group.getNote()));

        // 范围 / 归属变化时，检查现有引用是否跨范围（如果有，拒绝更新）
        boolean scopeChanged = !scope.equals(existing.getScope());
        boolean loraChanged = scope.equals("lora") && loraId != null && !loraId.equals(existing.getLoraId());
        if (scopeChanged || loraChanged) {
            List<GroupRef> existingRefs = groupMapper.listRefs(id);
            for (GroupRef ref : existingRefs) {
                TagGroup refGroup = groupMapper.findById(ref.getRefGroupId());
                if (refGroup == null) continue;
                // 检查新范围是否能引用旧引用组
                if (!canReference(target.getScope(), target.getLoraId(), refGroup.getScope(), refGroup.getLoraId())) {
                    throw new BizException("范围变更后，现有引用「" + refGroup.getName() + "」将跨范围，请先解除引用");
                }
            }
            // 还要检查引用我的那些组——它们的范围是否还能引用我（其实引用我的组不会被我改范围影响，这里跳过）
        }

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
        requireGroup(id);
        // 编辑区正在使用的词组块不能删，否则编辑区渲染会静默丢内容
        long workspaceCount = groupMapper.countWorkspaceRefs(id);
        if (workspaceCount > 0) {
            throw new BizException("该词组被 " + workspaceCount + " 个编辑区引用，请先在编辑区移除后再删除");
        }
        // 删除保护：被其他组引用时禁止删除，返回引用明细供前端展示
        long incomingCount = groupMapper.countIncomingRefs(id);
        if (incomingCount > 0) {
            List<GroupRef> incomingRefs = groupMapper.listIncomingRefs(id);
            throw new BizException("该词组被 " + incomingCount + " 个词组引用（" +
                    String.join("、", incomingRefs.stream()
                            .map(r -> r.getRefGroupName() != null ? r.getRefGroupName() : "组#" + r.getGroupId())
                            .toArray(String[]::new)) +
                    "），请先解除所有引用后再删除");
        }
        // 联动清理引用关系（我引用别人的 + 别人引用我的——上面已经拦住了别人引用我的情况，这里只清理我引用别人的）
        groupMapper.deleteRefsByGroup(id);
        // 清理条目
        itemMapper.deleteByGroup(id);
        // 最后删除组
        groupMapper.delete(id);
    }

    /** 复制词组：保留所有条目，名称加"副本"；不复制引用关系（用户可手动添加） */
    @Transactional
    public TagGroup duplicate(Long id) {
        TagGroup source = requireGroup(id);
        TagGroup copy = new TagGroup();
        copy.setName(source.getName() + " 副本");
        copy.setKind(source.getKind());
        copy.setScope(source.getScope());
        copy.setLoraId(source.getLoraId());
        copy.setCategory(source.getCategory());
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

    // --- 引用关系管理 ---

    /** 添加引用：校验范围、环检测 */
    public GroupRef addRef(Long groupId, Long refGroupId, Integer priority, Integer sortOrder) {
        TagGroup group = requireGroup(groupId);
        if (refGroupId == null || refGroupId.equals(groupId)) {
            throw new BizException("不能引用自己");
        }
        TagGroup refGroup = groupMapper.findById(refGroupId);
        if (refGroup == null) {
            throw new BizException("被引用的词组不存在，可能已被删除");
        }
        // 范围校验
        if (!canReference(group.getScope(), group.getLoraId(), refGroup.getScope(), refGroup.getLoraId())) {
            throw new BizException("引用词组范围不一致（需同为全局或同一 LoRA）");
        }
        // 查重
        if (groupMapper.countRefExists(groupId, refGroupId) > 0) {
            throw new BizException("已经引用过这个词组了");
        }
        // 环检测：DFS 查 refGroup 的引用链里有没有 groupId
        if (createsCycle(groupId, refGroupId)) {
            throw new BizException("引用会形成循环（例如 A→B→A）");
        }

        // 默认优先级/顺序
        int prio = priority != null ? priority : groupMapper.maxPriority(groupId) + 1;
        int order = sortOrder != null ? sortOrder : groupMapper.maxRefSortOrder(groupId) + 1;

        GroupRef ref = new GroupRef();
        ref.setGroupId(groupId);
        ref.setRefGroupId(refGroupId);
        ref.setPriority(prio);
        ref.setSortOrder(order);
        groupMapper.insertRef(ref);
        return ref;
    }

    /** 更新引用的优先级/顺序 */
    public void updateRef(Long groupId, Long refId, Integer priority, Integer sortOrder) {
        requireGroup(groupId);
        GroupRef existing = findRefOrThrow(groupId, refId);
        GroupRef update = new GroupRef();
        update.setId(refId);
        update.setGroupId(groupId);
        update.setPriority(priority != null ? priority : existing.getPriority());
        update.setSortOrder(sortOrder != null ? sortOrder : existing.getSortOrder());
        groupMapper.updateRef(update);
    }

    /** 删除引用 */
    public void removeRef(Long groupId, Long refId) {
        requireGroup(groupId);
        findRefOrThrow(groupId, refId); // 确保引用存在且属于当前组
        groupMapper.deleteRef(refId, groupId);
    }

    /** DFS 环检测：从 refGroup 出发，沿它的引用链向上，看能不能遇到 groupId */
    private boolean createsCycle(Long groupId, Long refGroupId) {
        Set<Long> visited = new HashSet<>();
        List<Long> queue = new ArrayList<>();
        queue.add(refGroupId);
        int depth = 0;
        while (!queue.isEmpty() && depth < MAX_REF_DEPTH) {
            List<Long> next = new ArrayList<>();
            for (Long cur : queue) {
                if (cur.equals(groupId)) {
                    return true; // 回到起点，成环
                }
                if (!visited.add(cur)) {
                    continue;
                }
                // 查 cur 引用了谁，继续向上
                for (GroupRef r : groupMapper.listRefs(cur)) {
                    next.add(r.getRefGroupId());
                }
            }
            queue = next;
            depth++;
        }
        return false;
    }

    /** 范围是否匹配：global 只能引用 global；lora 只能引用同 lora */
    private boolean canReference(String myScope, Long myLoraId, String refScope, Long refLoraId) {
        if (!myScope.equals(refScope)) {
            return false;
        }
        if ("lora".equals(myScope)) {
            return myLoraId != null && myLoraId.equals(refLoraId);
        }
        return true;
    }

    private GroupRef findRefOrThrow(Long groupId, Long refId) {
        List<GroupRef> refs = groupMapper.listRefs(groupId);
        for (GroupRef ref : refs) {
            if (ref.getId().equals(refId)) {
                return ref;
            }
        }
        throw new BizException("引用不存在，可能已被删除");
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

    /**
     * 从另一个词组整块导入条目：保留原正负向与权重，已存在的（同 tag + 同 polarity）跳过。
     */
    @Transactional
    public Map<String, Object> importItems(Long targetId, Long sourceId) {
        requireGroup(targetId);
        TagGroup source = requireGroup(sourceId);
        if (targetId.equals(sourceId)) {
            throw new BizException("不能从自己导入");
        }

        int added = 0;
        int skipped = 0;
        for (TagGroupItem src : itemMapper.listByGroup(sourceId)) {
            if (src.getTagId() == null) {
                skipped++;
                continue;
            }
            if (itemMapper.countDuplicate(targetId, src.getTagId(), src.getPolarity()) > 0) {
                skipped++;
                continue;
            }
            TagGroupItem copy = new TagGroupItem();
            copy.setGroupId(targetId);
            copy.setTagId(src.getTagId());
            copy.setPolarity(src.getPolarity());
            copy.setWeight(src.getWeight());
            copy.setSortOrder(itemMapper.maxSortOrder(targetId, src.getPolarity()) + 1);
            itemMapper.insert(copy);
            added++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceName", source.getName());
        result.put("added", added);
        result.put("skipped", skipped);
        return result;
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
        String category = group.getCategory() == null || group.getCategory().isBlank()
                ? "other"
                : group.getCategory().trim();
        if (!settingService.hasGroupCategory(category)) {
            throw new BizException("未知词组分类：" + category);
        }
        group.setCategory(category);
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
