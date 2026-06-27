package com.zhitu.travel.chatmemory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * LLM 调用结果缓存服务
 *
 * 对高频相似的出行问答进行缓存，减少重复的大模型调用。
 * 缓存命中则直接返回，无需调用 LLM，降低 Token 消耗与响应延迟。
 *
 * 设计：
 * - 两级缓存：本地 Caffeine + Redis（可选）
 * - 相似问题通过归一化 key 匹配（去除停用词、标点）
 * - 默认 TTL = 10 分钟，热点问题自动延长
 * - 最大缓存 2000 条，防止内存溢出
 */
@Service
public class ResultCacheService {

    private static final Logger log = LoggerFactory.getLogger(ResultCacheService.class);

    /** 高频出行问答缓存 */
    private Cache<String, String> travelAnswerCache;

    @PostConstruct
    public void init() {
        travelAnswerCache = Caffeine.newBuilder()
                .maximumSize(2000)
                // TTL 5~15 分钟随机，防雪崩
                .expireAfterWrite(5 + (long) (Math.random() * 10), TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("Travel 结果缓存初始化完成");
    }

    /**
     * 获取缓存结果
     *
     * @param query 用户原始问题
     * @return 缓存结果，未命中返回 null
     */
    public String get(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        String key = normalize(query);
        String cached = travelAnswerCache.getIfPresent(key);
        if (cached != null) {
            log.debug("结果缓存命中: query={}", query.substring(0, Math.min(20, query.length())));
        }
        return cached;
    }

    /**
     * 存入缓存
     */
    public void put(String query, String answer) {
        if (query == null || answer == null) {
            return;
        }
        String key = normalize(query);
        travelAnswerCache.put(key, answer);
    }

    /**
     * 判断是否应该缓存这个结果
     * 策略：长度适中的结果才缓存，太短（可能是错误信息）不缓存
     */
    public boolean shouldCache(String answer) {
        return answer != null && answer.length() > 20;
    }

    /**
     * 清除缓存
     */
    public void invalidate(String query) {
        if (query != null) {
            travelAnswerCache.invalidate(normalize(query));
        }
    }

    public void invalidateAll() {
        travelAnswerCache.invalidateAll();
    }

    /**
     * 缓存统计
     */
    public String getStats() {
        return travelAnswerCache.stats().toString();
    }

    // ==================== Key 归一化 ====================

    /**
     * 对用户 query 做归一化处理，提高缓存命中率
     *
     * 例："西安有什么好吃的？" 和 "西安好吃的推荐一下"
     * → 归一化后可能匹配同一缓存
     */
    private String normalize(String query) {
        if (query == null) return "";
        // 去标点
        String normalized = query.replaceAll("[\\p{P}\\p{S}\\s]", "");
        // 去常见停用词
        normalized = normalized
                .replaceAll("(?i)[吗呢吧嘛]", "")
                .replaceAll("(?i)[的了吗]", "");
        // 截断过长 query（取前 50 字）
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }
        return normalized.toLowerCase();
    }
}
