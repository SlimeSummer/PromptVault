package com.promptvault.mapper;

import com.promptvault.entity.Lora;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LoraMapper {

    List<Lora> list(@Param("keyword") String keyword);

    Lora findById(@Param("id") Long id);

    long countByName(@Param("name") String name,
                     @Param("excludeId") Long excludeId);

    int insert(Lora lora);

    int update(Lora lora);

    int delete(@Param("id") Long id);

    /** 删除 LoRA 时解除标签归属，避免留下悬空引用 */
    int clearTagReferences(@Param("id") Long id);
}
