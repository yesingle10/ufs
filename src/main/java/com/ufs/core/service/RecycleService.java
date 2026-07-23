package com.ufs.core.service;

import com.ufs.core.common.PageResult;
import com.ufs.core.model.dto.object.ObjectDto;

/**
 * 回收站服务（软删除对象的可恢复/彻底删除，对齐控制台回收站）。
 */
public interface RecycleService {

    PageResult<ObjectDto> list(int limit);

    void restore(String fileId);

    void purge(String fileId);
}
