package com.zhitu.travel.config;

import com.zhitu.travel.chatmemory.RedisChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * Redis 与缓存配置
 */
@Configuration
public class TravelRedisConfig {

    @Bean
    public RedisTemplate<String, Serializable> redisCacheTemplate(
            LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(lettuceConnectionFactory);
        return template;
    }

    /**
     * 基于 Redis 的对话记忆
     * 替代 MessageWindowChatMemory（内存版）
     * TTL = 30 分钟无交互自动过期
     */
    @Bean
    public ChatMemory redisChatMemory(StringRedisTemplate stringRedisTemplate) {
        return new RedisChatMemory(stringRedisTemplate, 1800);
    }
}
