package com.zhitu.travel.chatmemory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的对话记忆实现
 *
 * 替代 Spring AI 的 MessageWindowChatMemory（内存窗口），
 * 支持多实例共享会话上下文，配合 TTL 管理会话生命周期。
 *
 * 设计：
 * - 每个 conversationId 对应一个 Redis List，顺序存储消息
 * - 消息先序列化为 JSON 字符串再存入
 * - TTL 无交互 30 分钟后自动过期，防止内存泄漏
 * - 支持最大消息数限制（滑动窗口）
 */
public class RedisChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemory.class);

    /** 会话前缀 */
    private static final String CONVERSATION_PREFIX = "chat:session:";

    /** 默认 TTL（秒） */
    private static final long DEFAULT_TTL_SECONDS = 1800; // 30 分钟

    /** 最大消息数 */
    private static final int MAX_MESSAGES = 50;

    private final StringRedisTemplate redisTemplate;

    private final long ttlSeconds;

    public RedisChatMemory(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_TTL_SECONDS);
    }

    public RedisChatMemory(StringRedisTemplate redisTemplate, long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }

        String key = buildKey(conversationId);

        // 将 Message 序列化为 JSON 字符串
        List<String> serializedMessages = messages.stream()
                .map(this::serialize)
                .collect(Collectors.toList());

        // 批量追加到 List 尾部
        redisTemplate.opsForList().rightPushAll(key, serializedMessages);

        // 限制消息数量，从头部裁剪
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_MESSAGES) {
            redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
        }

        // 刷新 TTL
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public List<Message> get(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }

        String key = buildKey(conversationId);

        // 读取全量消息
        List<String> serializedMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (serializedMessages == null || serializedMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 反序列化
        List<Message> messages = new ArrayList<>(serializedMessages.size());
        for (String json : serializedMessages) {
            Message msg = deserialize(json);
            if (msg != null) {
                messages.add(msg);
            }
        }

        // 读取时刷新 TTL
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

        return messages;
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }
        redisTemplate.delete(buildKey(conversationId));
    }

    /**
     * 获取会话消息数量
     */
    public Long size(String conversationId) {
        return redisTemplate.opsForList().size(buildKey(conversationId));
    }

    private String buildKey(String conversationId) {
        return CONVERSATION_PREFIX + conversationId;
    }

    // ==================== 序列化/反序列化 ====================
    //
    // Message 体系复杂（UserMessage / AssistantMessage / ToolResponseMessage 等），
    // 这里用简单的 "type:content" 格式存储。
    // 生产环境建议用 Jackson 全序列化。

    private String serialize(Message message) {
        String type = message.getMessageType().getValue();
        String content = message.getText() != null ? message.getText() : "";
        // 转义内容中的冒号和换行
        content = content.replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("\n", "\\n");
        return type + ":" + content;
    }

    private Message deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            // 找到第一个未转义的冒号作为分隔
            int colonIndex = -1;
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                if (c == ':' && (i == 0 || data.charAt(i - 1) != '\\')) {
                    colonIndex = i;
                    break;
                }
            }
            if (colonIndex == -1) {
                return null;
            }
            String type = data.substring(0, colonIndex);
            String content = data.substring(colonIndex + 1)
                    .replace("\\n", "\n")
                    .replace("\\:", ":")
                    .replace("\\\\", "\\");

            // 根据类型重建 Message
            switch (type) {
                case "user":
                    return new org.springframework.ai.chat.messages.UserMessage(content);
                case "assistant":
                    return new org.springframework.ai.chat.messages.AssistantMessage(content);
                case "system":
                    return new org.springframework.ai.chat.messages.SystemMessage(content);
                default:
                    log.warn("未知消息类型: {}, 按 user 处理", type);
                    return new org.springframework.ai.chat.messages.UserMessage(content);
            }
        } catch (Exception e) {
            log.error("消息反序列化失败: {}", data, e);
            return null;
        }
    }
}
