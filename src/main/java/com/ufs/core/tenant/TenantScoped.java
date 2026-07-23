package com.ufs.core.tenant;

/**
 * 标记接口：Mapper 参数对象实现它即声明“已携带 tenantId”。
 * {@link TenantSqlInterceptor} 据此强制业务表语句必须带租户上下文（对齐 SQL 规范 §7）。
 */
public interface TenantScoped {

    String getTenantId();
}
