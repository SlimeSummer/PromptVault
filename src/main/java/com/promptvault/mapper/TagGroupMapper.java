package com.promptvault.mapper;

import com.promptvault.entity.TagGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagGroupMapper {

    /** 列表支持 scope 过滤 + keyword 模糊匹配名称 */
    List<TagGroup> list(@Param("scope") String scope,
                        @Param("loraId") Long loraId,
                        @Param("keyword") String keyword);

    TagGroup findById(@Param("id") Long id);

    long countByLora(@Param("loraId") Long loraId);

    long countByTag(@Param("tagId") Long tagId);

    int maxSortOrder(@Param("scope") String scope, @Param("loraId") Long loraId);

    int insert(TagGroup group);

    int update(TagGroup group);

    int delete(@Param("id") Long id);
}
