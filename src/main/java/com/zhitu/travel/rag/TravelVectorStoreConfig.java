package com.zhitu.travel.rag;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 旅游攻略向量数据库配置（初始化基于内存的向量数据库 Bean）
 */
@Configuration
public class TravelVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(TravelVectorStoreConfig.class);

    @Resource
    private TravelDocumentLoader travelDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore travelVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        try {
            // 加载文档
            List<Document> documentList = travelDocumentLoader.loadMarkdowns();
            // 自动补充关键词元信息
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
            simpleVectorStore.add(enrichedDocuments);
        } catch (Exception e) {
            log.warn("旅游文档向量化失败（API Key 可能无效），应用仍可正常启动: {}", e.getMessage());
        }
        return simpleVectorStore;
    }
}
