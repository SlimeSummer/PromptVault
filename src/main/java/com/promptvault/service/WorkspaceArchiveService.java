package com.promptvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.common.BizException;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.WorkspaceArchiveRequest;
import com.promptvault.entity.Lora;
import com.promptvault.entity.Preset;
import com.promptvault.entity.Tag;
import com.promptvault.entity.TagGroup;
import com.promptvault.entity.Workspace;
import com.promptvault.entity.WorkspaceArchive;
import com.promptvault.entity.WorkspaceItem;
import com.promptvault.mapper.LoraMapper;
import com.promptvault.mapper.PresetMapper;
import com.promptvault.mapper.TagGroupMapper;
import com.promptvault.mapper.TagMapper;
import com.promptvault.mapper.WorkspaceArchiveMapper;
import com.promptvault.mapper.WorkspaceItemMapper;
import com.promptvault.mapper.WorkspaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编辑区存档：把当前配置与条目引用序列化成 JSON 快照，
 * 存档独立列表管理，避免存档数量把编辑区下拉框挤爆；支持 JSON 导入 / 导出。
 */
@Service
public class WorkspaceArchiveService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final String TYPE_TAG = "tag";
    private static final String TYPE_GROUP = "group";
    private static final String POLARITY_POS = "pos";
    private static final String POLARITY_NEG = "neg";
    private static final String NEGATIVE_CATEGORY = "negative";
    private static final String ARTIST_CATEGORY = "artist";
    private static final double MIN_WEIGHT = 0.1;
    private static final double MAX_WEIGHT = 2.0;

    private final WorkspaceArchiveMapper archiveMapper;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceItemMapper itemMapper;
    private final LoraMapper loraMapper;
    private final PresetMapper presetMapper;
    private final TagMapper tagMapper;
    private final TagGroupMapper groupMapper;
    private final SettingService settingService;
    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;

    public WorkspaceArchiveService(WorkspaceArchiveMapper archiveMapper,
                                   WorkspaceMapper workspaceMapper,
                                   WorkspaceItemMapper itemMapper,
                                   LoraMapper loraMapper,
                                   PresetMapper presetMapper,
                                   TagMapper tagMapper,
                                   TagGroupMapper groupMapper,
                                   SettingService settingService,
                                   WorkspaceService workspaceService,
                                   ObjectMapper objectMapper) {
        this.archiveMapper = archiveMapper;
        this.workspaceMapper = workspaceMapper;
        this.itemMapper = itemMapper;
        this.loraMapper = loraMapper;
        this.presetMapper = presetMapper;
        this.tagMapper = tagMapper;
        this.groupMapper = groupMapper;
        this.settingService = settingService;
        this.workspaceService = workspaceService;
        this.objectMapper = objectMapper;
    }

    public List<WorkspaceArchive> list() {
        return archiveMapper.list();
    }

    /** 保存当前编辑区为一条存档 */
    public WorkspaceArchive save(WorkspaceArchiveRequest request) {
        if (request == null || request.getWorkspaceId() == null) {
            throw new BizException("请选择要保存的编辑区");
        }
        Workspace workspace = workspaceMapper.findById(request.getWorkspaceId());
        if (workspace == null) {
            throw new BizException("编辑区不存在，可能已被删除");
        }

        String name = text(request.getName());
        if (name.isEmpty()) {
            name = workspace.getName() + " 存档";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BizException("存档名称过长（最多 " + MAX_NAME_LENGTH + " 字符）");
        }

        List<WorkspaceItem> items = itemMapper.listByWorkspace(workspace.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", 1);
        payload.put("name", name);
        payload.put("note", blankToNull(request.getNote()));
        payload.put("sourceWorkspaceName", workspace.getName());
        payload.put("workspace", workspaceSnapshot(workspace));
        payload.put("items", itemSnapshots(items));

        Map<String, Object> rendered = workspaceService.render(workspace.getId());
        payload.put("positiveText", rendered.get("positiveText"));
        payload.put("negativeText", rendered.get("negativeText"));

        WorkspaceArchive archive = new WorkspaceArchive();
        archive.setName(name);
        archive.setNote(blankToNull(request.getNote()));
        archive.setSourceWorkspaceId(workspace.getId());
        archive.setSourceWorkspaceName(workspace.getName());
        archive.setItemCount((long) items.size());
        archive.setPayload(writeJson(payload));
        archiveMapper.insert(archive);
        return archiveMapper.findById(archive.getId());
    }

    /** 存档详情：列表字段 + 解析后的 payload（前端导出用） */
    public Map<String, Object> detail(Long id) {
        WorkspaceArchive archive = requireArchive(id);
        Map<String, Object> result = summary(archive);
        result.put("payload", readJson(archive.getPayload()));
        return result;
    }

    public void delete(Long id) {
        requireArchive(id);
        archiveMapper.delete(id);
    }

    /**
     * 导入 JSON 存档：只做校验与入库，不直接覆盖编辑区，用户可在列表中按需读取。
     */
    public WorkspaceArchive importArchive(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BizException("导入内容不能为空");
        }
        if (!(body.get("items") instanceof List)) {
            throw new BizException("存档内容缺少 items 列表");
        }
        if (!(body.get("workspace") instanceof Map)) {
            throw new BizException("存档内容缺少 workspace 配置");
        }

        String name = text(body.get("name"));
        if (name.isEmpty()) {
            name = "导入存档";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        Map<String, Object> payload = new LinkedHashMap<>(body);
        payload.put("version", body.getOrDefault("version", 1));
        payload.put("name", name);
        payload.putIfAbsent("note", null);

        WorkspaceArchive archive = new WorkspaceArchive();
        archive.setName(name);
        archive.setNote(blankToNull(text(body.get("note"))));
        archive.setSourceWorkspaceName(blankToNull(text(body.get("sourceWorkspaceName"))));
        archive.setItemCount((long) ((List<?>) body.get("items")).size());
        archive.setPayload(writeJson(payload));
        archiveMapper.insert(archive);
        return archiveMapper.findById(archive.getId());
    }

    /**
     * 读取存档：覆盖目标编辑区的配置与全部条目。
     * 失效的 LoRA / 预设 / 词条 / 跨 LoRA 词组会被跳过并在结果里计数。
     */
    @Transactional
    public Map<String, Object> restore(Long archiveId, Long workspaceId) {
        WorkspaceArchive archive = requireArchive(archiveId);
        if (workspaceId == null) {
            throw new BizException("请选择要读取到的编辑区");
        }
        Workspace workspace = workspaceMapper.findById(workspaceId);
        if (workspace == null) {
            throw new BizException("编辑区不存在，可能已被删除");
        }
        Map<String, Object> payload = readJson(archive.getPayload());
        Map<String, Object> config = asMap(payload.get("workspace"));

        Long loraId = longValue(config.get("loraId"));
        Lora lora = loraId == null ? null : loraMapper.findById(loraId);
        if (lora == null) {
            loraId = null;
        }

        Long presetId = loraId == null ? null : longValue(config.get("presetId"));
        Preset preset = presetId == null ? null : presetMapper.findById(presetId);
        if (preset == null || preset.getLoraId() == null || !preset.getLoraId().equals(loraId)) {
            presetId = null;
        }

        Workspace target = new Workspace();
        target.setId(workspace.getId());
        target.setName(workspace.getName());
        target.setLoraId(loraId);
        target.setPresetId(presetId);
        target.setNote(blankToNull(text(config.get("note"))));
        workspaceMapper.update(target);

        itemMapper.deleteByWorkspace(workspaceId);

        List<?> rows = payload.get("items") instanceof List ? (List<?>) payload.get("items") : List.of();
        Set<String> seen = new HashSet<>();
        int added = 0;
        int skipped = 0;
        int order = 10;
        for (Object raw : rows) {
            Map<String, Object> row = asMap(raw);
            String itemType = text(row.get("itemType"));
            Long refId = longValue(row.get("refId"));
            if (refId == null) {
                skipped++;
                continue;
            }
            String polarity = normalizePolarity(row.get("polarity"));

            if (TYPE_TAG.equals(itemType)) {
                Tag tag = tagMapper.findById(refId);
                if (tag == null || !seen.add(TYPE_TAG + "|" + refId + "|" + polarity)) {
                    skipped++;
                    continue;
                }
                WorkspaceItem created = new WorkspaceItem();
                created.setWorkspaceId(workspaceId);
                created.setItemType(TYPE_TAG);
                created.setRefId(refId);
                created.setPolarity(polarity);
                created.setWeight(clampWeight(row.get("weight")));
                created.setSectionOverride(sanitizeSection(row.get("sectionOverride")));
                created.setSortOrder(order);
                itemMapper.insert(created);
                added++;
                order += 10;
            } else if (TYPE_GROUP.equals(itemType)) {
                TagGroup group = groupMapper.findById(refId);
                if (group == null) {
                    skipped++;
                    continue;
                }
                if ("lora".equals(group.getScope())
                        && group.getLoraId() != null
                        && (loraId == null || !group.getLoraId().equals(loraId))) {
                    skipped++;
                    continue;
                }
                if (!seen.add(TYPE_GROUP + "|" + refId + "|" + POLARITY_POS)) {
                    skipped++;
                    continue;
                }
                WorkspaceItem created = new WorkspaceItem();
                created.setWorkspaceId(workspaceId);
                created.setItemType(TYPE_GROUP);
                created.setRefId(refId);
                created.setPolarity(POLARITY_POS);
                created.setWeight(1.0);
                created.setSectionOverride(sanitizeSection(row.get("sectionOverride")));
                created.setSortOrder(order);
                itemMapper.insert(created);
                added++;
                order += 10;
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("archiveName", archive.getName());
        result.put("workspaceId", workspaceId);
        result.put("added", added);
        result.put("skipped", skipped);
        return result;
    }

    // --- 辅助 ---

    private WorkspaceArchive requireArchive(Long id) {
        if (id == null) {
            throw new BizException("存档不存在，可能已被删除");
        }
        WorkspaceArchive archive = archiveMapper.findById(id);
        if (archive == null) {
            throw new BizException("存档不存在，可能已被删除");
        }
        return archive;
    }

    private Map<String, Object> summary(WorkspaceArchive archive) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", archive.getId());
        result.put("name", archive.getName());
        result.put("note", archive.getNote());
        result.put("sourceWorkspaceId", archive.getSourceWorkspaceId());
        result.put("sourceWorkspaceName", archive.getSourceWorkspaceName());
        result.put("itemCount", archive.getItemCount());
        result.put("createdAt", archive.getCreatedAt());
        return result;
    }

    private Map<String, Object> workspaceSnapshot(Workspace workspace) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", workspace.getName());
        config.put("loraId", workspace.getLoraId());
        config.put("loraName", workspace.getLoraName());
        config.put("presetId", workspace.getPresetId());
        config.put("presetName", workspace.getPresetName());
        config.put("note", workspace.getNote());
        return config;
    }

    private List<Map<String, Object>> itemSnapshots(List<WorkspaceItem> items) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkspaceItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemType", item.getItemType());
            row.put("refId", item.getRefId());
            row.put("polarity", item.getPolarity());
            row.put("weight", item.getWeight());
            row.put("sectionOverride", item.getSectionOverride());
            row.put("sortOrder", item.getSortOrder());
            rows.add(row);
        }
        return rows;
    }

    private String sanitizeSection(Object value) {
        String section = text(value);
        if (section.isEmpty()
                || NEGATIVE_CATEGORY.equals(section)
                || ARTIST_CATEGORY.equals(section)) {
            return null;
        }
        for (CategoryOption option : settingService.getCategories()) {
            if (section.equals(option.getValue())) {
                return section;
            }
        }
        return null;
    }

    private String normalizePolarity(Object value) {
        String polarity = text(value);
        return POLARITY_NEG.equals(polarity) ? POLARITY_NEG : POLARITY_POS;
    }

    private double clampWeight(Object value) {
        double weight = 1.0;
        if (value instanceof Number) {
            weight = ((Number) value).doubleValue();
        } else {
            String raw = text(value);
            if (!raw.isEmpty()) {
                try {
                    weight = Double.parseDouble(raw);
                } catch (NumberFormatException ignored) {
                    weight = 1.0;
                }
            }
        }
        if (weight < MIN_WEIGHT) {
            return MIN_WEIGHT;
        }
        if (weight > MAX_WEIGHT) {
            return MAX_WEIGHT;
        }
        return weight;
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BizException("存档内容已损坏，无法解析");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("存档内容序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String raw = text(value);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
