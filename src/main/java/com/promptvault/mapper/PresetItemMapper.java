package com.promptvault.mapper;

import com.promptvault.entity.PresetItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PresetItemMapper {

    List<PresetItem> listByPreset(@Param("presetId") Long presetId);

    PresetItem findById(@Param("id") Long id);

    long countDuplicate(@Param("presetId") Long presetId,
                        @Param("tagId") Long tagId,
                        @Param("polarity") String polarity);

    int maxSortOrder(@Param("presetId") Long presetId,
                     @Param("polarity") String polarity);

    int insert(PresetItem item);

    int update(PresetItem item);

    int updateSort(@Param("id") Long id,
                   @Param("sortOrder") int sortOrder);

    int delete(@Param("id") Long id);

    int deleteByPreset(@Param("presetId") Long presetId);
}
