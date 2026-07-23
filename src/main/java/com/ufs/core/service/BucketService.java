package com.ufs.core.service;

import com.ufs.core.model.dto.bucket.BucketDto;
import com.ufs.core.model.dto.bucket.CreateBucketRequest;
import com.ufs.core.model.dto.bucket.UpdateBucketRequest;

import java.util.List;

/**
 * 存储桶业务接口（对齐 OpenAPI Bucket tag）。
 */
public interface BucketService {

    List<BucketDto> list();

    BucketDto get(String name);

    BucketDto create(CreateBucketRequest req);

    BucketDto update(String name, UpdateBucketRequest req);

    void delete(String name);
}
