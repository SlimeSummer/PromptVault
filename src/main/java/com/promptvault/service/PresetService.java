package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.dto.PresetItemOrderRequest;
import com.promptvault.entity.Lora;
import com.promptvault.entity.Preset;
import com.promptvault.entity.PresetItem;
import com.promptvault.entity.Tag;
import com.promptvault.mapper.LoraMapper;
import com.promptvault.mapper.PresetItemMapper;
import com.promptvault.mapper.PresetMapper;
import com.promptvault.mapper.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PresetService {

    public static final List<String> STATUSES = List.of("draft", "verified", "deprecated");
    public static final List<String> POLARITIES = List.of("pos", "neg");

    private final PresetMapper presetMapper;
    private final PresetItemMapper itemMapper;
    private final LoraMapper loraMapper;
    private final TagMapper tagMapper;
    private final ImageService imageService;

    public PresetService(PresetMapper presetMapper,
                         PresetItemMapper itemMapper,
                         LoraMapper loraMapper,
                         TagMapper tagMapper,
                         ImageService imageService) {
        this.presetMapper = presetMapper;
        this.itemMapper = itemMapper;
        this.loraMapper = loraMapper;
        this.tagMapper = tagMapper;
        this.imageService = imageService;
    }

    public List<Preset> listByLora(Long loraId) {
        requireLora(loraId);
        return presetMapper.listByLora(loraId);
    }

    public Map<String, Object> detail(Long id) {
        Preset preset = requirePreset(id);
        List<PresetItem> items = itemMapper.listByPreset(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preset", preset);
        result.put("items", items);
        return result;
    }

    public Preset create(Preset preset) {
        if (preset.getLoraId() == null) {
            throw new BizException("请先选择所属 LoRA");
        }
        requireLora(preset.getLoraId());
        normalize(preset);
        preset.setSortOrder(presetMapper.maxSortOrder(preset.getLoraId()) + 1);
        presetMapper.insert(preset);
        return presetMapper.findById(preset.getId());
    }

    public Preset update(Long id, Preset preset) {
        Preset existing = requirePreset(id);
        normalize(preset);
        preset.setId(id);
        preset.setLoraId(existing.getLoraId());
        preset.setSortOrder(existing.getSortOrder());
        presetMapper.update(preset);
        return presetMapper.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        requirePreset(id);
        long workspaceCount = presetMapper.countWorkspaceRefs(id);
        if (workspaceCount > 0) {
            throw new BizException("该预设被 " + workspaceCount + " 个编辑区使用，请先更换或删除编辑区后再试");
        }
        itemMapper.deleteByPreset(id);
        imageService.deleteByRef("preset", id);
        presetMapper.delete(id);
    }

    @Transactional
    public Preset duplicate(Long id) {
        Preset source = requirePreset(id);
        Preset copy = new Preset();
        copy.setLoraId(source.getLoraId());
        copy.setName(source.getName() + " 副本");
        copy.setStatus("draft");
        copy.setNote(source.getNote());
        copy.setSortOrder(presetMapper.maxSortOrder(source.getLoraId()) + 1);
        presetMapper.insert(copy);

        for (PresetItem item : itemMapper.listByPreset(id)) {
            PresetItem copied = new PresetItem();
            copied.setPresetId(copy.getId());
            copied.setTagId(item.getTagId());
            copied.setPolarity(item.getPolarity());
            copied.setWeight(item.getWeight());
            copied.setSortOrder(item.getSortOrder());
            itemMapper.insert(copied);
        }
        return presetMapper.findById(copy.getId());
    }

    public PresetItem addItem(Long presetId, PresetItem item) {
        requirePreset(presetId);
        if (item.getTagId() == null) {
            throw new BizException("请选择要添加的词条");
        }
        Tag tag = tagMapper.findById(item.getTagId());
        if (tag == null) {
            throw new BizException("词条不存在，可能已被删除");
        }
        String polarity = normalizePolarity(item.getPolarity());
        if (itemMapper.countDuplicate(presetId, item.getTagId(), polarity) > 0) {
            throw new BizException("该词条已在" + polarityLabel(polarity) + "列表中");
        }

        PresetItem created = new PresetItem();
        created.setPresetId(presetId);
        created.setTagId(item.getTagId());
        created.setPolarity(polarity);
        created.setWeight(normalizeWeight(item.getWeight()));
        created.setSortOrder(itemMapper.maxSortOrder(presetId, polarity) + 1);
        itemMapper.insert(created);
        return itemMapper.findById(created.getId());
    }

    public PresetItem updateItem(Long presetId, Long itemId, PresetItem item) {
        requirePreset(presetId);
        PresetItem existing = requireItem(presetId, itemId);

        String polarity = item.getPolarity() == null
                ? existing.getPolarity()
                : normalizePolarity(item.getPolarity());
        int sortOrder = existing.getSortOrder();
        if (!polarity.equals(existing.getPolarity())) {
            if (itemMapper.countDuplicate(presetId, existing.getTagId(), polarity) > 0) {
                throw new BizException("该词条已在" + polarityLabel(polarity) + "列表中");
            }
            sortOrder = itemMapper.maxSortOrder(presetId, polarity) + 1;
        }

        PresetItem update = new PresetItem();
        update.setId(itemId);
        update.setPolarity(polarity);
        update.setWeight(item.getWeight() == null
                ? existing.getWeight()
                : normalizeWeight(item.getWeight()));
        update.setSortOrder(sortOrder);
        itemMapper.update(update);
        return itemMapper.findById(itemId);
    }

    public void deleteItem(Long presetId, Long itemId) {
        requirePreset(presetId);
        requireItem(presetId, itemId);
        itemMapper.delete(itemId);
    }

    @Transactional
    public void reorder(Long presetId, PresetItemOrderRequest request) {
        requirePreset(presetId);
        if (request == null || request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new BizException("排序内容为空");
        }
        int order = 0;
        for (Long itemId : request.getItemIds()) {
            requireItem(presetId, itemId);
            itemMapper.updateSort(itemId, order++);
        }
    }

    private void normalize(Preset preset) {
        if (preset.getName() == null || preset.getName().trim().isEmpty()) {
            throw new BizException("预设名称不能为空");
        }
        preset.setName(preset.getName().trim());
        if (preset.getName().length() > 100) {
            throw new BizException("预设名称过长（最多 100 字符）");
        }

        String status = preset.getStatus() == null ? "draft" : preset.getStatus().trim();
        if (!STATUSES.contains(status)) {
            throw new BizException("未知状态：" + status);
        }
        preset.setStatus(status);
        preset.setNote(blankToNull(preset.getNote()));
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

    private Preset requirePreset(Long id) {
        Preset preset = presetMapper.findById(id);
        if (preset == null) {
            throw new BizException("预设不存在，可能已被删除");
        }
        return preset;
    }

    private PresetItem requireItem(Long presetId, Long itemId) {
        PresetItem item = itemMapper.findById(itemId);
        if (item == null || !presetId.equals(item.getPresetId())) {
            throw new BizException("预设条目不存在，可能已被删除");
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
