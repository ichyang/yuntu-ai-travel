package com.zhitu.travel.app;

import com.zhitu.travel.advisor.MyLoggerAdvisor;
import com.zhitu.travel.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class TravelPlannerApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是一位专业的智能出行规划专家。你的目标是帮助用户制定完美的旅行方案。

            你的工作流程：
            1. 需求分析：了解用户的出行目的地、预算、天数、偏好（美食/人文/自然/休闲）
            2. 信息检索：使用工具查询景点信息、天气、交通、住宿等
            3. 方案规划：综合分析信息，制定详细的每日行程
            4. 结果输出：生成结构化的旅行方案文档

            回答风格要求：
            - 热情、细致，充分考虑用户偏好
            - 行程要具体到每天上午/下午/晚上
            - 包含预算分配建议（门票、住宿、餐饮、交通）
            - 附上实用小贴士（当地美食推荐、避坑提醒）
            """;

    public TravelPlannerApp(ChatModel dashscopeChatModel) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /** 基础对话（支持多轮对话记忆） */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /** SSE 流式对话 */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream().content();
    }

    /** 生成结构化旅行方案 */
    public TravelPlan doChatWithPlan(String message, String chatId) {
        TravelPlan plan = chatClient.prompt()
                .system(SYSTEM_PROMPT + "请生成结构化的旅行方案，包含行程安排和预算明细")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().entity(TravelPlan.class);
        log.info("travelPlan: {}", plan);
        return plan;
    }

    record TravelPlan(String destination, Integer budget, Integer days,
                      List<DayPlan> itinerary, BudgetDetail budgetDetail) {}
    record DayPlan(Integer day, String morning, String afternoon, String evening) {}
    record BudgetDetail(Integer accommodation, Integer transportation, Integer food, Integer tickets, Integer other) {}

    // ===== RAG 知识库问答（旅游攻略知识库） =====

    @Resource
    private VectorStore travelVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient.prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(travelVectorStore))
                .call().chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    // ===== 工具调用（联网搜索景点、天气等） =====

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call().chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    // ===== MCP 服务调用 =====

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call().chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
