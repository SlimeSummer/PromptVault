package com.promptvault.mapper;

import com.promptvault.entity.Workspace;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkspaceMapper {

    List<Workspace> list();

    Workspace findById(@Param("id") Long id);

    int insert(Workspace workspace);

    int update(Workspace workspace);

    int delete(@Param("id") Long id);
}
