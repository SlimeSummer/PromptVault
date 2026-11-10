package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.dto.CategoryOption;
import com.promptvault.dto.TagBatchRequest;
import com.promptvault.entity.Tag;
import com.promptvault.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TagService {

    /** 匹配 (tag:1.2) 这种权重写法，导入时把权重剥掉只留标签本体 */
    private static final Pattern WEIGHT_PATTERN =
            Pattern.compile("^\\(\\s*(.+?)\\s*:\\s*-?\\d+(?:\\.\\d+)?\\s*\\)$");

    /** 表头可排序字段白名单，其余一律回退到 updated_at */
    private static final Set<String> SORT_FIELDS = Set.of("prompt", "nameZh", "category");

    private final TagMapper tagMapper;
    private final SettingService settingService;
    private final TagClassifier classifier;
    private final ImageService imageService;

    public TagService(TagMapper tagMapper, SettingService settingService, TagClassifier classifier,
                      ImageService imageService) {
        this.tagMapper = tagMapper;
        this.settingService = settingService;
        this.classifier = classifier;
        this.imageService = imageService;
    }

    public Map<String, Object> page(String keyword, String category, String sort, String order,
                                    Boolean favorite, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        String kw = keyword == null ? null : keyword.trim();
        String cat = category == null ? null : category.trim();
        String safeSort = sort != null && SORT_FIELDS.contains(sort) ? sort : null;
        String safeOrder = "asc".equalsIgnoreCase(order) ? "asc" : "desc";

        long total = tagMapper.count(kw, cat, favorite);
        List<Tag> list = tagMapper.list(kw, cat, safeSort, safeOrder, favorite,
                (safePage - 1) * safeSize, safeSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    /** 切换收藏状态，返回切换后的状态 */
    public boolean toggleFavorite(Long id) {
        if (tagMapper.findById(id) == null) {
            throw new BizException("词条不存在，可能已被删除");
        }
        if (tagMapper.countFavorite(id) > 0) {
            tagMapper.deleteFavorite(id);
            return false;
        }
        tagMapper.insertFavorite(id);
        return true;
    }

    public List<Map<String, Object>> suggest(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return tagMapper.suggest(kw, safeLimit);
    }

    public Map<String, Object> stats() {
        Map<String, Object> byCategory = new LinkedHashMap<>();
        for (CategoryOption option : settingService.getCategories()) {
            byCategory.put(option.getValue(), 0L);
        }
        long total = 0;
        for (Map<String, Object> row : tagMapper.countByCategory()) {
            Object key = row.get("category");
            Object cnt = row.get("cnt");
            if (key == null) {
                continue;
            }
            long value = cnt == null ? 0L : ((Number) cnt).longValue();
            byCategory.put(key.toString(), value);
            total += value;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("categories", byCategory);
        return result;
    }

    public Tag create(Tag tag) {
        normalize(tag);
        if (tagMapper.countByPrompt(tag.getPrompt(), null) > 0) {
            throw new BizException("提示词已存在：" + tag.getPrompt());
        }
        tagMapper.insert(tag);
        return tagMapper.findById(tag.getId());
    }

    public Tag update(Long id, Tag tag) {
        Tag existing = tagMapper.findById(id);
        if (existing == null) {
            throw new BizException("词条不存在，可能已被删除");
        }
        normalize(tag);
        if (tagMapper.countByPrompt(tag.getPrompt(), id) > 0) {
            throw new BizException("提示词已存在：" + tag.getPrompt());
        }
        tag.setId(id);
        tagMapper.update(tag);
        return tagMapper.findById(id);
    }

    public void delete(Long id) {
        Tag existing = tagMapper.findById(id);
        if (existing == null) {
            throw new BizException("词条不存在，可能已被删除");
        }
        long refs = tagMapper.countReferences(id);
        if (refs > 0) {
            throw new BizException("该词条正被 " + refs + " 处引用（预设/词组/编辑区），请先解除引用");
        }
        tagMapper.deleteFavorite(id);
        tagMapper.delete(id);
        imageService.deleteByRef("tag", id);
    }

    /**
     * 批量删除。逐条做引用检查；任一被引用则全部回滚（@Transactional）。
     *
     * @return 删除条数
     */
    @Transactional
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("未选择任何词条");
        }
        int ok = 0;
        List<String> blocked = new java.util.ArrayList<>();
        for (Long id : ids) {
            Tag existing = tagMapper.findById(id);
            if (existing == null) {
                continue;
            }
            long refs = tagMapper.countReferences(id);
            if (refs > 0) {
                blocked.add(existing.getPrompt() + "（被 " + refs + " 处引用）");
                continue;
            }
            tagMapper.deleteFavorite(id);
            tagMapper.delete(id);
            imageService.deleteByRef("tag", id);
            ok++;
        }
        if (!blocked.isEmpty()) {
            throw new BizException("以下词条仍被引用，无法删除：" + String.join("；", blocked));
        }
        return ok;
    }

    @Transactional
    public Map<String, Object> batchImport(TagBatchRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new BizException("导入内容为空");
        }
        boolean autoCategory = Boolean.TRUE.equals(request.getAutoCategory());
        String category = null;
        if (!autoCategory) {
            category = request.getCategory() == null ? "other" : request.getCategory().trim();
            if (!settingService.hasCategory(category)) {
                throw new BizException("未知分类：" + category);
            }
        }

        Set<String> candidates = new LinkedHashSet<>();
        for (String raw : request.getText().split("[,，、\\r\\n]+")) {
            String prompt = stripWeight(raw.trim());
            if (!prompt.isEmpty()) {
                candidates.add(prompt);
            }
        }

        Map<String, Map<String, Integer>> byCategory = new LinkedHashMap<>();
        int inserted = 0;
        int skipped = 0;
        for (String prompt : candidates) {
            String targetCategory = autoCategory ? classifier.classify(prompt) : category;
            if (!settingService.hasCategory(targetCategory)) {
                targetCategory = "other";
            }

            Map<String, Integer> stat = byCategory.computeIfAbsent(targetCategory, k -> {
                Map<String, Integer> m = new LinkedHashMap<>();
                m.put("inserted", 0);
                m.put("skipped", 0);
                return m;
            });

            if (tagMapper.countByPrompt(prompt, null) > 0) {
                skipped++;
                stat.merge("skipped", 1, Integer::sum);
                continue;
            }
            Tag tag = new Tag();
            tag.setPrompt(prompt);
            tag.setCategory(targetCategory);
            tag.setLoraId(request.getLoraId());
            tagMapper.insert(tag);
            inserted++;
            stat.merge("inserted", 1, Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", candidates.size());
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        result.put("autoCategory", autoCategory);
        result.put("byCategory", byCategory);
        return result;
    }

    private void normalize(Tag tag) {
        if (tag.getPrompt() == null || tag.getPrompt().trim().isEmpty()) {
            throw new BizException("提示词不能为空");
        }
        String prompt = stripWeight(tag.getPrompt().trim());
        if (prompt.isEmpty()) {
            throw new BizException("提示词不能为空");
        }
        if (prompt.length() > 200) {
            throw new BizException("提示词过长（最多 200 字符）");
        }
        tag.setPrompt(prompt);

        String category = tag.getCategory() == null ? "other" : tag.getCategory().trim();
        if (!settingService.hasCategory(category)) {
            throw new BizException("未知分类：" + category);
        }
        tag.setCategory(category);

        tag.setNameZh(blankToNull(tag.getNameZh()));
        tag.setNote(blankToNull(tag.getNote()));
    }

    private String stripWeight(String value) {
        Matcher matcher = WEIGHT_PATTERN.matcher(value);
        return matcher.matches() ? matcher.group(1).trim() : value;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
