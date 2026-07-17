-- ============================================
-- 智途 AI 出行规划 - PostgreSQL + PGVector 初始化
-- ============================================
-- 使用方法：
--   1. 安装 PostgreSQL 14+
--   2. 安装 PGVector 插件（https://github.com/pgvector/pgvector）
--   3. 在 psql 或 Navicat 中执行本脚本
-- ============================================

-- 创建数据库（如未创建）
-- 注意：docker-compose 已通过 POSTGRES_DB 环境变量自动创建，此处无需重复创建
-- CREATE DATABASE zhitu_travel ENCODING 'UTF8';

-- 切换到 zhitu_travel 数据库后执行：
-- \c zhitu_travel

-- 启用 PGVector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 验证扩展安装
SELECT * FROM pg_extension WHERE extname = 'vector';

-- ============================================
-- vector_store 表
-- 由 Spring AI PgVectorStore 自动创建（initializeSchema=true）
-- 如果自动创建失败，可以手动执行以下建表语句
-- ============================================

-- CREATE TABLE IF NOT EXISTS vector_store (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     content TEXT,
--     metadata JSONB,
--     embedding VECTOR(1536)   -- DashScope 通义千问的向量维度
-- );

-- 创建 HNSW 索引（加速相似度检索）
-- CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx
--     ON vector_store
--     USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 200);

-- 提示：Spring AI 的 PgVectorStore 会自动创建上述表结构和索引
-- 建表配置在 PgVectorVectorStoreConfig.java 中：
--   .initializeSchema(true)
--   .indexType(HNSW)
--   .distanceType(COSINE_DISTANCE)

-- ============================================
-- 验证：查询已存入的向量数量
-- ============================================
-- SELECT COUNT(*) FROM vector_store;

-- ============================================
-- 验证：相似度搜索
-- ============================================
-- SELECT content, 1 - (embedding <=> '[0.1, 0.2, ...]'::vector) AS cosine_similarity
-- FROM vector_store
-- ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
-- LIMIT 5;
