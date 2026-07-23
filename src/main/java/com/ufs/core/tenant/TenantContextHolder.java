package com.ufs.core.tenant;

/**
 * 租户上下文（ThreadLocal）。由认证拦截器在请求入口设置，
 * service / repository 层据此取 tenantId（对齐 PRD §18 多租户、SQL 规范 §7）。
 * 禁止跨线程传递；请求结束必须 clear()。
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Context> CTX = ThreadLocal.withInitial(Context::empty);

    private TenantContextHolder() {
    }

    public static void set(Context ctx) {
        CTX.set(ctx);
    }

    public static Context get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }

    public static String getTenantId() {
        return CTX.get().tenantId();
    }

    public static String getOperatorId() {
        return CTX.get().operatorId();
    }

    public static String getRequestId() {
        return CTX.get().requestId();
    }

    public static String getAppKey() {
        return CTX.get().appKey();
    }

    /** 内部服务调用可临时置为系统上下文（绕过租户强绑，仅限审计/运维场景）。 */
    public record Context(String tenantId, String operatorId, String requestId, String appKey, boolean system) {
        public static Context empty() {
            return new Context(null, null, null, null, false);
        }

        public static Context of(String tenantId, String operatorId, String requestId, String appKey) {
            return new Context(tenantId, operatorId, requestId, appKey, false);
        }

        public static Context system(String requestId) {
            return new Context(null, "system", requestId, "system", true);
        }
    }
}
