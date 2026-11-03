package com.promptvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.common.BizException;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.SettingsRequest;
import com.promptvault.mapper.SettingMapper;
import com.promptvault.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SettingService {

    private static final Logger log = LoggerFactory.getLogger(SettingService.class);

    public static final String KEY_TAG_CATEGORIES = "tag.categories";
    public static final String KEY_LORA_BASE_MODELS = "lora.base_models";

    private static final int MAX_CATEGORIES = 30;
    private static final int MAX_BASE_MODELS = 30;
    private static final Pattern VALUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,40}$");

    public static final List<CategoryOption> DEFAULT_CATEGORIES = List.of(
            new CategoryOption("trigger", "触发词"),
            new CategoryOption("quality", "质量词"),
            new CategoryOption("style", "风格"),
            new CategoryOption("outfit", "穿搭"),
            new CategoryOption("scene", "场景"),
            new CategoryOption("action", "动作"),
            new CategoryOption("expression", "表情"),
            new CategoryOption("detail", "细节"),
            new CategoryOption("negative", "负面"),
            new CategoryOption("other", "其他"));

    public static final List<String> DEFAULT_BASE_MODELS = List.of(
            "sd15", "sdxl", "pony", "illustrious", "noobai", "other");

    private final SettingMapper settingMapper;
    private final TagMapper tagMapper;
    private final ObjectMapper objectMapper;

    public SettingService(SettingMapper settingMapper, TagMapper tagMapper, ObjectMapper objectMapper) {
        this.settingMapper = settingMapper;
        this.tagMapper = tagMapper;
        this.objectMapper = objectMapper;
    }

    public SettingsRequest load() {
        SettingsRequest result = new SettingsRequest();
        result.setTagCategories(readCategories());
        result.setLoraBaseModels(readBaseModels());
        return result;
    }

    public List<CategoryOption> getCategories() {
        return readCategories();
    }

    public List<String> getBaseModels() {
        return readBaseModels();
    }

    public boolean hasCategory(String value) {
        if (value == null) {
            return false;
        }
        for (CategoryOption option : readCategories()) {
            if (value.equals(option.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public SettingsRequest save(SettingsRequest request) {
        List<CategoryOption> categories = request == null ? null : request.getTagCategories();
        List<String> baseModels = request == null ? null : request.getLoraBaseModels();
        validateCategories(categories);
        validateBaseModels(baseModels);

        settingMapper.upsert(KEY_TAG_CATEGORIES, toJson(categories));
        settingMapper.upsert(KEY_LORA_BASE_MODELS, toJson(baseModels));
        return load();
    }

    private List<CategoryOption> readCategories() {
        String raw = settingMapper.findByKey(KEY_TAG_CATEGORIES);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>(DEFAULT_CATEGORIES);
        }
        try {
            List<CategoryOption> parsed = objectMapper.readValue(raw, new TypeReference<List<CategoryOption>>() {
            });
            if (parsed == null || parsed.isEmpty()) {
                return new ArrayList<>(DEFAULT_CATEGORIES);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("分类设置解析失败，回退默认值", e);
            return new ArrayList<>(DEFAULT_CATEGORIES);
        }
    }

    private List<String> readBaseModels() {
        String raw = settingMapper.findByKey(KEY_LORA_BASE_MODELS);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>(DEFAULT_BASE_MODELS);
        }
        try {
            List<String> parsed = objectMapper.readValue(raw, new TypeReference<List<String>>() {
            });
            if (parsed == null || parsed.isEmpty()) {
                return new ArrayList<>(DEFAULT_BASE_MODELS);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("底模设置解析失败，回退默认值", e);
            return new ArrayList<>(DEFAULT_BASE_MODELS);
        }
    }

    private void validateCategories(List<CategoryOption> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new BizException("至少保留一个词条分类");
        }
        if (categories.size() > MAX_CATEGORIES) {
            throw new BizException("分类数量最多 " + MAX_CATEGORIES + " 个");
        }

        Set<String> values = new LinkedHashSet<>();
        for (CategoryOption option : categories) {
            String value = option.getValue() == null ? "" : option.getValue().trim();
            String label = option.getLabel() == null ? "" : option.getLabel().trim();
            if (!VALUE_PATTERN.matcher(value).matches()) {
                throw new BizException("分类标识只能用字母、数字、下划线或短横线（1~40 字符）：" + value);
            }
            if (label.isEmpty()) {
                throw new BizException("分类显示名称不能为空：" + value);
            }
            if (label.length() > 20) {
                throw new BizException("分类显示名称最多 20 个字符：" + label);
            }
            if (!values.add(value)) {
                throw new BizException("分类标识重复：" + value);
            }
            option.setValue(value);
            option.setLabel(label);
        }

        // 分类被词条使用时不允许删除，避免出现无处归属的词条
        Map<String, Long> usage = usageCounts();
        for (Map.Entry<String, Long> entry : usage.entrySet()) {
            if (!values.contains(entry.getKey())) {
                throw new BizException("分类「" + entry.getKey() + "」下还有 " + entry.getValue()
                        + " 条词条，请先移动这些词条再删除");
            }
        }
    }

    private void validateBaseModels(List<String> baseModels) {
        if (baseModels == null || baseModels.isEmpty()) {
            throw new BizException("至少保留一个底模选项");
        }
        if (baseModels.size() > MAX_BASE_MODELS) {
            throw new BizException("底模数量最多 " + MAX_BASE_MODELS + " 个");
        }

        Set<String> values = new HashSet<>();
        for (String raw : baseModels) {
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                throw new BizException("底模名称不能为空");
            }
            if (value.length() > 40) {
                throw new BizException("底模名称最多 40 个字符：" + value);
            }
            if (!values.add(value)) {
                throw new BizException("底模名称重复：" + value);
            }
        }
    }

    private Map<String, Long> usageCounts() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : tagMapper.countByCategory()) {
            Object key = row.get("category");
            Object cnt = row.get("cnt");
            if (key != null) {
                counts.put(key.toString(), cnt == null ? 0L : ((Number) cnt).longValue());
            }
        }
        return counts;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("设置序列化失败：" + e.getMessage());
        }
    }
}
