package com.ufs.core.repository.mapper;

import com.ufs.core.repository.entity.UploadTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * upload_task 访问层（对齐 PRD §8.2 / SQL 规范 §4）。分片上传事务；过期清理走系统上下文。
 */
@Mapper
public interface UploadTaskMapper {

    @Insert("""
            INSERT INTO upload_task (upload_id, file_id, tenant_id, provider, bucket, object_key, upload_mode,
                cloud_upload_id, total_size, part_size, total_parts, completed_parts, status, expires_at, created_at)
            VALUES (#{uploadId}, #{fileId}, #{tenantId}, #{provider}, #{bucket}, #{objectKey}, #{uploadMode},
                #{cloudUploadId}, #{totalSize}, #{partSize}, #{totalParts}, #{completedParts}, #{status},
                #{expiresAt}, #{createdAt})
            """)
    int insert(UploadTask t);

    @Select("SELECT * FROM upload_task WHERE tenant_id=#{tenantId} AND upload_id=#{uploadId}")
    UploadTask selectById(@Param("tenantId") String tenantId, @Param("uploadId") String uploadId);

    @Select("SELECT * FROM upload_task WHERE tenant_id=#{tenantId} AND file_id=#{fileId} AND status=0")
    UploadTask selectActiveByFile(@Param("tenantId") String tenantId, @Param("fileId") String fileId);

    @Update("UPDATE upload_task SET status=#{status}, completed_parts=#{completedParts}, updated_at=#{updatedAt} "
            + "WHERE tenant_id=#{tenantId} AND upload_id=#{uploadId}")
    int updateProgress(@Param("tenantId") String tenantId, @Param("uploadId") String uploadId,
                       @Param("status") int status, @Param("completedParts") String completedParts,
                       @Param("updatedAt") long updatedAt);

    @Update("UPDATE upload_task SET status=#{status} WHERE tenant_id=#{tenantId} AND upload_id=#{uploadId}")
    int updateStatus(@Param("tenantId") String tenantId, @Param("uploadId") String uploadId,
                     @Param("status") int status);

    @Delete("DELETE FROM upload_task WHERE tenant_id=#{tenantId} AND upload_id=#{uploadId}")
    int delete(@Param("tenantId") String tenantId, @Param("uploadId") String uploadId);

    /** 过期事务清理（系统上下文执行，豁免租户守卫）。 */
    @Select("SELECT * FROM upload_task WHERE expires_at < #{now} AND status IN (0,1)")
    List<UploadTask> selectExpired(@Param("now") long now);
}
