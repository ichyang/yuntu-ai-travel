package com.zhitu.travel.agent;

import com.zhitu.travel.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 知途 AI 出行规划智能体（拥有自主规划能力）
 */
@Component
public class TravelPlannerAgent extends ToolCallAgent {

    public TravelPlannerAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("travelPlanner");
        String SYSTEM_PROMPT = """
                You are TravelPlanner, an intelligent travel planning assistant.
                Your goal is to help users create perfect travel itineraries.
                You have various tools at your disposal that you can call upon.
                For complex tasks, break down the problem and use different tools step by step.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                Break down complex travel planning tasks into steps: search attractions → check weather → plan route → generate document.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
