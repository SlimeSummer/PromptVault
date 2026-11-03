package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.entity.Lora;
import com.promptvault.mapper.LoraMapper;
import com.promptvault.mapper.PresetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class LoraService {

    public static final List<String> SOURCES = List.of("self_trained", "third_party");

    /** 表头可排序字段白名单，其余一律回退到 updated_at */
    private static final Set<String> SORT_FIELDS =
            Set.of("name", "baseModel", "triggerWords", "presetCount");

    private final LoraMapper loraMapper;
    private final PresetMapper presetMapper;

    public LoraService(LoraMapper loraMapper, PresetMapper presetMapper) {
        this.loraMapper = loraMapper;
        this.presetMapper = presetMapper;
    }

    public List<Lora> list(String keyword, String sort, String order) {
        String kw = keyword == null ? null : keyword.trim();
        String safeSort = sort != null && SORT_FIELDS.contains(sort) ? sort : null;
        String safeOrder = "asc".equalsIgnoreCase(order) ? "asc" : "desc";
        return loraMapper.list(kw, safeSort, safeOrder);
    }

    public Lora get(Long id) {
        Lora lora = loraMapper.findById(id);
        if (lora == null) {
            throw new BizException("LoRA 不存在，可能已被删除");
        }
        return lora;
    }

    public Lora create(Lora lora) {
        normalize(lora);
        if (loraMapper.countByName(lora.getName(), null) > 0) {
            throw new BizException("同名 LoRA 已存在：" + lora.getName());
        }
        loraMapper.insert(lora);
        return loraMapper.findById(lora.getId());
    }

    public Lora update(Long id, Lora lora) {
        Lora existing = get(id);
        normalize(lora);
        if (loraMapper.countByName(lora.getName(), id) > 0) {
            throw new BizException("同名 LoRA 已存在：" + lora.getName());
        }
        lora.setId(id);
        if (lora.getCoverPath() == null) {
            lora.setCoverPath(existing.getCoverPath());
        }
        loraMapper.update(lora);
        return loraMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        long presetCount = presetMapper.countByLora(id);
        if (presetCount > 0) {
            throw new BizException("该 LoRA 下还有 " + presetCount + " 个预设，请先删除预设");
        }
        loraMapper.clearTagReferences(id);
        loraMapper.delete(id);
    }

    private void normalize(Lora lora) {
        if (lora.getName() == null || lora.getName().trim().isEmpty()) {
            throw new BizException("LoRA 名称不能为空");
        }
        lora.setName(lora.getName().trim());
        if (lora.getName().length() > 100) {
            throw new BizException("LoRA 名称过长（最多 100 字符）");
        }

        String source = lora.getSource() == null ? "third_party" : lora.getSource().trim();
        if (!SOURCES.contains(source)) {
            throw new BizException("未知来源：" + source);
        }
        lora.setSource(source);

        lora.setBaseModel(blankToNull(lora.getBaseModel()));
        lora.setTriggerWords(blankToNull(lora.getTriggerWords()));
        lora.setNote(blankToNull(lora.getNote()));
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
