package com.promptvault.mapper;

import com.promptvault.entity.GroupRef;
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

    /** 所有全局负面词组（编辑区导出负向词时作为第一层） */
    List<TagGroup> listGlobalNegative();

    /** 被多少个编辑区作为词组块引用 */
    long countWorkspaceRefs(@Param("groupId") Long groupId);

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

    // --- 引用关系 group_ref ---

    /** 当前组引用的所有组（按 priority + sort_order 排序） */
    List<GroupRef> listRefs(@Param("groupId") Long groupId);

    /** 引用了当前组的所有其他组（删除保护用） */
    List<GroupRef> listIncomingRefs(@Param("refGroupId") Long refGroupId);

    /** 当前组被引用的次数 */
    long countIncomingRefs(@Param("refGroupId") Long refGroupId);

    /** 查重：当前组是否已引用过目标组 */
    long countRefExists(@Param("groupId") Long groupId, @Param("refGroupId") Long refGroupId);

    int insertRef(GroupRef ref);

    int updateRef(GroupRef ref);

    int deleteRef(@Param("id") Long id, @Param("groupId") Long groupId);

    /** 删除当前组的所有引用（组删除时联动清理） */
    int deleteRefsByGroup(@Param("groupId") Long groupId);

    /** 当前组引用的最大 sort_order（用于新引用追加） */
    int maxRefSortOrder(@Param("groupId") Long groupId);

    /** 当前组引用的最小 priority（用于默认新增优先级） */
    Integer maxPriority(@Param("groupId") Long groupId);
}
