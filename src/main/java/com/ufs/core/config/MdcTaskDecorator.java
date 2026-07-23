package com.ufs.core.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 异步任务装饰器：把父线程 MDC（requestId/tenantId）传递到子线程，保证链路可观测（对齐 Java 规范 §9）。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        return () -> {
            if (ctx != null) {
                MDC.setContextMap(ctx);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
