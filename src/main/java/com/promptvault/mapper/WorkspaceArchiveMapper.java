package com.promptvault.mapper;

import com.promptvault.entity.WorkspaceArchive;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkspaceArchiveMapper {

    /** 列表不查 payload，避免一次性把大 JSON 全拉出来 */
    List<WorkspaceArchive> list();

    WorkspaceArchive findById(@Param("id") Long id);

    int insert(WorkspaceArchive archive);

    int delete(@Param("id") Long id);
}
