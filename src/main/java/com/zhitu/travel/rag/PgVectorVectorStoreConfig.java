package com.zhitu.travel.rag;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

// PgVector 向量存储配置（已启用）
// 需要先启动 PostgreSQL 并创建 zhitu_travel 数据库
// 首次启动会自动建表（initializeSchema=true）
@Configuration
public class PgVectorVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorVectorStoreConfig.class);
    private static final int BATCH_SIZE = 20;

    @Resource
    private TravelDocumentLoader travelDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();
        // 加载文档（分批处理，避免 API 限制）
        List<Document> documents = travelDocumentLoader.loadMarkdowns();
        int total = documents.size();
        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            List<Document> batch = documents.subList(i, end);
            try {
                vectorStore.add(batch);
                log.info("已加载文档 {}/{}", end, total);
            } catch (Exception e) {
                log.warn("批量加载文档时出错 ({}/{}): {}", i, total, e.getMessage());
            }
        }
        return vectorStore;
    }
}
