package com.ufs.core.tenant;

import com.ufs.core.common.error.UfsErrorCode;
import com.ufs.core.common.error.UfsException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * MyBatis 拦截器：强制业务表语句带租户上下文（对齐 SQL 规范 §7、PRD §18）。
 * 机制：解析实际 SQL，若涉及 file_meta / upload_task / storage_policy / audit_log，
 * 则要求参数对象实现 {@link TenantScoped} 或携带 tenantId，否则拦截并抛 UFS.4030。
 * 系统上下文（Context.system）可豁免（仅审计/运维内部调用）。
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class TenantSqlInterceptor implements Interceptor {

    private static final Set<String> BUSINESS_TABLES = Set.of(
            "file_meta", "upload_task", "storage_policy", "audit_log");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (TenantContextHolder.get().system()) {
            return invocation.proceed();
        }
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object param = invocation.getArgs()[1];
        String sql = ms.getBoundSql(param).getSql().toLowerCase();
        boolean touchesBusiness = BUSINESS_TABLES.stream().anyMatch(t -> sql.contains(t));
        if (touchesBusiness && !hasTenantScope(param)) {
            throw new UfsException(UfsErrorCode.FORBIDDEN, Map.of("reason", "tenant scope required"));
        }
        return invocation.proceed();
    }

    private boolean hasTenantScope(Object param) {
        if (param == null) {
            return false;
        }
        if (param instanceof TenantScoped scoped) {
            return scoped.getTenantId() != null && !scoped.getTenantId().isBlank();
        }
        if (param instanceof Map<?, ?> map) {
            return map.containsKey("tenantId") && map.get("tenantId") != null;
        }
        return false;
    }
}
