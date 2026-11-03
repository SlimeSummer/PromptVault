package com.promptvault.mapper;

import com.promptvault.entity.Tag;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TagMapper {

    List<Tag> list(@Param("keyword") String keyword,
                   @Param("category") String category,
                   @Param("sort") String sort,
                   @Param("order") String order,
                   @Param("offset") int offset,
                   @Param("size") int size);

    long count(@Param("keyword") String keyword,
               @Param("category") String category);

    /** 搜索补全：按前缀优先返回已有词条 */
    List<Map<String, Object>> suggest(@Param("keyword") String keyword,
                                      @Param("limit") int limit);

    List<Map<String, Object>> countByCategory();

    Tag findById(@Param("id") Long id);

    long countByPrompt(@Param("prompt") String prompt,
                       @Param("excludeId") Long excludeId);

    long countReferences(@Param("id") Long id);

    int insert(Tag tag);

    int update(Tag tag);

    int delete(@Param("id") Long id);
}
