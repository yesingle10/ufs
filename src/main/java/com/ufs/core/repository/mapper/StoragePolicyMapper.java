package com.ufs.core.repository.mapper;

import com.ufs.core.repository.entity.StoragePolicy;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * storage_policy 访问层（对齐 PRD §8.3 / SQL 规范 §4）。按 bizType 路由（UK(tenant_id, biz_type)）。
 */
@Mapper
public interface StoragePolicyMapper {

    @Insert("""
            INSERT INTO storage_policy (policy_id, tenant_id, biz_type, provider, bucket, path_template,
                max_file_size, allowed_types, lifecycle_days, cdn_domain, is_active, created_at)
            VALUES (#{policyId}, #{tenantId}, #{bizType}, #{provider}, #{bucket}, #{pathTemplate},
                #{maxFileSize}, #{allowedTypes}, #{lifecycleDays}, #{cdnDomain}, #{isActive}, #{createdAt})
            """)
    int insert(StoragePolicy p);

    @Select("SELECT * FROM storage_policy WHERE tenant_id=#{tenantId} AND policy_id=#{policyId}")
    StoragePolicy selectById(@Param("tenantId") String tenantId, @Param("policyId") String policyId);

    @Select("SELECT * FROM storage_policy WHERE tenant_id=#{tenantId} AND biz_type=#{bizType} AND is_active=1")
    StoragePolicy selectByBiz(@Param("tenantId") String tenantId, @Param("bizType") String bizType);

    @Select("SELECT * FROM storage_policy WHERE tenant_id=#{tenantId} AND is_active=1")
    List<StoragePolicy> listActive(@Param("tenantId") String tenantId);

    @Update("UPDATE storage_policy SET is_active=#{isActive}, created_at=#{createdAt} "
            + "WHERE tenant_id=#{tenantId} AND policy_id=#{policyId}")
    int setActive(@Param("tenantId") String tenantId, @Param("policyId") String policyId,
                  @Param("isActive") int isActive, @Param("createdAt") long createdAt);

    @Delete("DELETE FROM storage_policy WHERE tenant_id=#{tenantId} AND policy_id=#{policyId}")
    int delete(@Param("tenantId") String tenantId, @Param("policyId") String policyId);
}
