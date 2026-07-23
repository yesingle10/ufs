package com.ufs.core.repository.mapper;

import com.ufs.core.repository.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * audit_log 访问层（对齐 PRD §8.4 / SQL 规范 §4）。关键操作审计，落 ES 前先落 MySQL 兜底。
 */
@Mapper
public interface AuditLogMapper {

    @Insert("""
            INSERT INTO audit_log (tenant_id, operator_id, operator_type, action, file_id, object_key,
                client_ip, request_id, created_at)
            VALUES (#{tenantId}, #{operatorId}, #{operatorType}, #{action}, #{fileId}, #{objectKey},
                #{clientIp}, #{requestId}, #{createdAt})
            """)
    int insert(AuditLog a);

    @Select("""
            SELECT * FROM audit_log
            WHERE tenant_id=#{tenantId}
              AND (#{operatorId} IS NULL OR operator_id=#{operatorId})
              AND (#{action} IS NULL OR action=#{action})
              AND created_at <= #{cursor}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<AuditLog> listByCursor(@Param("tenantId") String tenantId, @Param("operatorId") String operatorId,
                                 @Param("action") String action, @Param("cursor") long cursor,
                                 @Param("limit") int limit);
}
