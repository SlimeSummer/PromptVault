package com.promptvault.mapper;

import com.promptvault.entity.RefImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RefImageMapper {

    int insert(RefImage image);

    RefImage findById(@Param("id") Long id);

    List<RefImage> listByRef(@Param("refType") String refType, @Param("refId") Long refId);

    int delete(@Param("id") Long id);

    int deleteByRef(@Param("refType") String refType, @Param("refId") Long refId);

    long countByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
