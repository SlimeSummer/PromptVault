package com.promptvault.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SettingMapper {

    List<Map<String, Object>> findAll();

    String findByKey(@Param("key") String key);

    int upsert(@Param("key") String key, @Param("value") String value);
}
