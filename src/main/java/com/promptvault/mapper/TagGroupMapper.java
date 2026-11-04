package com.promptvault.mapper;

import com.promptvault.entity.TagGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TagGroupMapper {

    /** 列表支持 scope / category 过滤 + keyword 模糊匹配名称 */
    List<TagGroup> list(@Param("scope") String scope,
                        @Param("loraId") Long loraId,
                        @Param("category") String category,
                        @Param("keyword") String keyword);

    /** 按名称前缀补全，返回名称列表 */
    List<String> suggest(@Param("keyword") String keyword, @Param("limit") int limit);

    /** 分类使用量统计，用于设置页禁止删除在用分类 */
    List<Map<String, Object>> countByCategory();

    TagGroup findById(@Param("id") Long id);

    long countByLora(@Param("loraId") Long loraId);

    long countByTag(@Param("tagId") Long tagId);

    int maxSortOrder(@Param("scope") String scope, @Param("loraId") Long loraId);

    int insert(TagGroup group);

    int update(TagGroup group);

    int delete(@Param("id") Long id);
}
