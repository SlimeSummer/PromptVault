package com.promptvault.mapper;

import com.promptvault.entity.Preset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PresetMapper {

    List<Preset> listByLora(@Param("loraId") Long loraId);

    Preset findById(@Param("id") Long id);

    long countByLora(@Param("loraId") Long loraId);

    int maxSortOrder(@Param("loraId") Long loraId);

    int insert(Preset preset);

    int update(Preset preset);

    int delete(@Param("id") Long id);
}
