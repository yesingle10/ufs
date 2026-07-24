-- 确保 utf8mb4 字符集（MySQL Docker 镜像有时默认 latin1）
ALTER DATABASE ufs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
