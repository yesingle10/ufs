package com.ufs.core.repository.mapper;

import com.ufs.core.repository.entity.FileMeta;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * file_meta 访问层（对齐 PRD §8.1 / SQL 规范 §4）。所有语句强制带 tenant_id（租户守卫拦截器）。
 * 列表用游标分页（created_at DESC, file_id DESC），避免深翻页（SQL 规范 §6）。
 */
@Mapper
public interface FileMetaMapper {

    @Insert("""
            INSERT INTO file_meta (file_id, tenant_id, biz_type, provider, bucket, object_key, file_name,
                file_size, content_type, md5, width, height, duration, url, status, custom_data,
                callback_url, callback_status, created_at, updated_at)
            VALUES (#{fileId}, #{tenantId}, #{bizType}, #{provider}, #{bucket}, #{objectKey}, #{fileName},
                #{fileSize}, #{contentType}, #{md5}, #{width}, #{height}, #{duration}, #{url}, #{status},
                #{customData}, #{callbackUrl}, #{callbackStatus}, #{createdAt}, #{updatedAt})
            """)
    int insert(FileMeta m);

    @Select("SELECT * FROM file_meta WHERE tenant_id=#{tenantId} AND object_key=#{objectKey} AND deleted_at IS NULL")
    FileMeta selectByKey(@Param("tenantId") String tenantId, @Param("objectKey") String objectKey);

    @Select("SELECT * FROM file_meta WHERE tenant_id=#{tenantId} AND file_id=#{fileId} AND deleted_at IS NULL")
    FileMeta selectById(@Param("tenantId") String tenantId, @Param("fileId") String fileId);

    @Update("UPDATE file_meta SET status=#{status}, updated_at=#{updatedAt} "
            + "WHERE tenant_id=#{tenantId} AND file_id=#{fileId}")
    int updateStatus(@Param("tenantId") String tenantId, @Param("fileId") String fileId,
                     @Param("status") int status, @Param("updatedAt") long updatedAt);

    @Update("UPDATE file_meta SET deleted_at=#{deletedAt}, status=3 "
            + "WHERE tenant_id=#{tenantId} AND file_id=#{fileId}")
    int softDelete(@Param("tenantId") String tenantId, @Param("fileId") String fileId,
                   @Param("deletedAt") long deletedAt);

    @Select("SELECT COUNT(*) FROM file_meta WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND status=1")
    long countActive(@Param("tenantId") String tenantId);

    @Select("""
            SELECT * FROM file_meta
            WHERE tenant_id=#{tenantId}
              AND (#{bizType} IS NULL OR biz_type=#{bizType})
              AND (#{provider} IS NULL OR provider=#{provider})
              AND (#{status} IS NULL OR status=#{status})
              AND created_at <= #{cursor} AND deleted_at IS NULL
            ORDER BY created_at DESC, file_id DESC
            LIMIT #{limit}
            """)
    List<FileMeta> listByCursor(@Param("tenantId") String tenantId, @Param("bizType") String bizType,
                                @Param("provider") String provider, @Param("status") Integer status,
                                @Param("cursor") long cursor, @Param("limit") int limit);

    @Select("SELECT * FROM file_meta WHERE tenant_id=#{tenantId} AND deleted_at IS NOT NULL "
            + "ORDER BY deleted_at DESC LIMIT #{limit}")
    List<FileMeta> listDeleted(@Param("tenantId") String tenantId, @Param("limit") int limit);

    @Update("UPDATE file_meta SET deleted_at=NULL, status=1, updated_at=#{updatedAt} "
            + "WHERE tenant_id=#{tenantId} AND file_id=#{fileId}")
    int restore(@Param("tenantId") String tenantId, @Param("fileId") String fileId, @Param("updatedAt") long updatedAt);

    @Delete("DELETE FROM file_meta WHERE tenant_id=#{tenantId} AND file_id=#{fileId}")
    int purge(@Param("tenantId") String tenantId, @Param("fileId") String fileId);

    @Select("SELECT COALESCE(SUM(file_size),0) FROM file_meta WHERE tenant_id=#{tenantId} AND deleted_at IS NULL AND status=1")
    long sumBytes(@Param("tenantId") String tenantId);
}
