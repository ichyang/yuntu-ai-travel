package com.zhitu.travel.mcp;

import com.zhitu.travel.mcp.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZhituMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhituMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider travelImageTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}
