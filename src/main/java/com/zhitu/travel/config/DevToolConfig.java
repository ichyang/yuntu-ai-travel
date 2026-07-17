package com.zhitu.travel.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地开发时提供空的 ToolCallbackProvider（禁用 MCP 后使用）
 */
@Configuration
public class DevToolConfig {

    @Bean
    public ToolCallbackProvider devToolCallbackProvider() {
        return new ToolCallbackProvider() {
            @Override
            public ToolCallback[] getToolCallbacks() {
                return new ToolCallback[0];
            }
        };
    }
}
