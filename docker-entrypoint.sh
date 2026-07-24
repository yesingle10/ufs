#!/bin/bash
set -e

# =============================================================================
# UFS Core Docker 入口脚本
# 等待 MySQL / Redis 就绪后启动 Spring Boot
# =============================================================================

log() { echo "[ufs-entrypoint] $(date '+%H:%M:%S') $*"; }

# ---- 等 MySQL ----
log "Waiting for MySQL at ${MYSQL_HOST:-mysql}:${MYSQL_PORT:-3306}..."
until timeout 3 bash -c "echo >/dev/tcp/${MYSQL_HOST:-mysql}/${MYSQL_PORT:-3306}" 2>/dev/null; do
    log "MySQL not ready yet — retrying in 3s..."
    sleep 3
done
log "MySQL is ready."

# ---- 等 Redis ----
log "Waiting for Redis at ${REDIS_HOST:-redis}:${REDIS_PORT:-6379}..."
until timeout 3 bash -c "echo >/dev/tcp/${REDIS_HOST:-redis}/${REDIS_PORT:-6379}" 2>/dev/null; do
    log "Redis not ready yet — retrying in 3s..."
    sleep 3
done
log "Redis is ready."

# ---- 启动 ----
log "Starting UFS Core on port ${SERVER_PORT:-8080}, profile=${SPRING_PROFILES_ACTIVE:-docker}..."
exec java -Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /app/ufs-core.jar
