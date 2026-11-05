package com.promptvault.mapper;

import com.promptvault.entity.WorkspaceItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkspaceItemMapper {

    List<WorkspaceItem> listByWorkspace(@Param("workspaceId") Long workspaceId);

    WorkspaceItem findById(@Param("id") Long id);

    long countDuplicate(@Param("workspaceId") Long workspaceId,
                        @Param("itemType") String itemType,
                        @Param("refId") Long refId,
                        @Param("polarity") String polarity,
                        @Param("excludeId") Long excludeId);

    int maxSortOrder(@Param("workspaceId") Long workspaceId);

    int insert(WorkspaceItem item);

    int update(WorkspaceItem item);

    int updateSort(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    int delete(@Param("id") Long id);

    int deleteByWorkspace(@Param("workspaceId") Long workspaceId);
}
