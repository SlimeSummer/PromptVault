package com.promptvault.mapper;

import com.promptvault.entity.Tag;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TagMapper {

    List<Tag> list(@Param("keyword") String keyword,
                   @Param("category") String category,
                   @Param("offset") int offset,
                   @Param("size") int size);

    long count(@Param("keyword") String keyword,
               @Param("category") String category);

    List<Map<String, Object>> countByCategory();

    Tag findById(@Param("id") Long id);

    long countByPrompt(@Param("prompt") String prompt,
                       @Param("excludeId") Long excludeId);

    long countReferences(@Param("id") Long id);

    int insert(Tag tag);

    int update(Tag tag);

    int delete(@Param("id") Long id);
}
