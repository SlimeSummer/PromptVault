package com.promptvault.mapper;

import com.promptvault.entity.TagGroupItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagGroupItemMapper {

    List<TagGroupItem> listByGroup(@Param("groupId") Long groupId);

    TagGroupItem findById(@Param("id") Long id);

    long countDuplicate(@Param("groupId") Long groupId,
                        @Param("tagId") Long tagId,
                        @Param("polarity") String polarity);

    int maxSortOrder(@Param("groupId") Long groupId,
                     @Param("polarity") String polarity);

    int insert(TagGroupItem item);

    int update(TagGroupItem item);

    int updateSort(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    int delete(@Param("id") Long id);

    int deleteByGroup(@Param("groupId") Long groupId);
}
