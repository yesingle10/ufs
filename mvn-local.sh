#!/usr/bin/env bash
# 本地 Maven 启动器（无需系统预装 mvn）。
# 首次运行会自动从 archive.apache.org 下载 Maven 3.9.9 到 target/maven（已被 .gitignore 忽略）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
MT="$ROOT/target/maven"
MVN_HOME="$MT/home/apache-maven-3.9.9"
BOOT="$MVN_HOME/boot/plexus-classworlds-2.8.0.jar"

if [ ! -f "$BOOT" ]; then
  echo "[mvn-local] Maven 未找到，正在下载 3.9.9 ..."
  mkdir -p "$MT"
  ZIP="$MT/maven.zip"
  curl -fSL -o "$ZIP" https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip
  rm -rf "$MVN_HOME"
  unzip -q -o "$ZIP" -d "$MT/home"
  echo "[mvn-local] 已安装到 $MVN_HOME"
fi

export MAVEN_OPTS="${MAVEN_OPTS:-Xmx1536m}"
exec java -classpath "$BOOT" \
  -Dmaven.home="$MVN_HOME" \
  -Dmaven.multiModuleProjectDirectory="$ROOT" \
  -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
