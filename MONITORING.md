# UFS Monitoring & Observability

Prometheus + Grafana + Alertmanager 监控栈，与 ufs-core 一键启动。

## Quick Start

```bash
cp .env.example .env
docker compose up -d
```

启动后访问：

| 服务 | URL | 默认凭据 |
|------|-----|---------|
| **UFS Core** | http://localhost:8080 | — |
| **Prometheus** | http://localhost:9090 | — |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Alertmanager** | http://localhost:9093 | — |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |

## Grafana Dashboard

Grafana 启动后自动加载 `UFS Overview` dashboard，位于 **Dashboards → UFS → UFS Overview**。

### Dashboard Panels

| Row | Panels | Metrics |
|-----|--------|---------|
| **Overview** | Uptime, CPU Usage, Heap Memory, Thread Count | `process.uptime`, `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_threads_live` |
| **HTTP** | Request Rate, Response Time (P50/P95/P99), Error Rate | `http_server_requests_seconds` |
| **JVM** | GC Pause Time, Heap Usage Trend, Thread States | `jvm_gc_pause_seconds`, `jvm_memory_*`, `jvm_threads_states` |
| **Business** | Active Buckets, Upload Ops, Presigned Rate | Application custom metrics |

## Metrics Endpoint

ufs-core 通过 Actuator 暴露以下端点：

```
/actuator/health        — 健康检查
/actuator/health/readiness  — K8s readiness probe
/actuator/health/liveness   — K8s liveness probe
/actuator/prometheus    — Prometheus scrape target
/actuator/metrics       — 全量 Micrometer metrics
```

## Alertmanager

告警规则通过 Alertmanager 路由，默认打印到控制台。生产环境建议配置 Webhook / 企业微信 / PagerDuty。

```yaml
# alertmanager.yml 示例 webhook 接收器
receivers:
  - name: 'wecom'
    webhook_configs:
      - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
```

## Kubernetes

使用 Helm Chart 部署时，Prometheus 通常由集群级 `prometheus-operator` 管理。ufs-core 的 ServiceMonitor 配置可通过 Helm values 启用：

```bash
helm install ufs ./helm/ufs \
  --set monitoring.enabled=true \
  --set monitoring.serviceMonitor.enabled=true
```

## Files

```
monitoring/
├── prometheus/prometheus.yml       # Scrape 配置
├── grafana/
│   ├── dashboards/ufs-overview.json  # Dashboard JSON (17 panels)
│   ├── dashboards/dashboard.yml      # Provisioning 配置
│   └── datasources/datasource.yml    # Prometheus 数据源
└── alertmanager/alertmanager.yml   # 告警路由
```
