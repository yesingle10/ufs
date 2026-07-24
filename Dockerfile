# =============================================================================
# UFS Core — 多阶段 Docker 构建
# 阶段1：Maven 编译 | 阶段2：JDK 17 精简运行
# =============================================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 下载依赖（利用 Docker layer cache）
RUN mvn dependency:go-offline -B -q
COPY src/ src/
RUN mvn package -DskipTests -B -q -DfinalName=ufs-core

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# 非 root 运行
RUN groupadd -r ufs && useradd -r -g ufs ufs

# 拷贝构建产物
COPY --from=builder /build/target/ufs-core.jar /app/ufs-core.jar

# 入口脚本（等待依赖服务就绪）
COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh && chown -R ufs:ufs /app

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
USER ufs
ENTRYPOINT ["/app/docker-entrypoint.sh"]
