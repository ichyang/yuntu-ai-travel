package com.zhitu.travel.app;

import com.zhitu.travel.advisor.MyLoggerAdvisor;
import com.zhitu.travel.chatmemory.ResultCacheService;
import com.zhitu.travel.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class TravelPlannerApp {

    private final ChatClient chatClient;

    /** Redis-based chat memory (was: MessageWindowChatMemory) */
    private final ChatMemory redisChatMemory;

    /** LLM 结果缓存，减少重复 API 调用 */
    private final ResultCacheService resultCache;

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

    public TravelPlannerApp(ChatModel dashscopeChatModel,
                            ChatMemory redisChatMemory,
                            ResultCacheService resultCache) {
        this.redisChatMemory = redisChatMemory;
        this.resultCache = resultCache;

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 使用 Redis 持久化会话（替代原来的内存版 MessageWindowChatMemory）
                        MessageChatMemoryAdvisor.builder(redisChatMemory)
                                .build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /** 基础对话（支持多轮对话记忆，缓存高频问答结果） */
    public String doChat(String message, String chatId) {
        // 1. 先查缓存（高频相似问答直接命中）
        String cached = resultCache.get(message);
        if (cached != null) {
            log.info("缓存命中，跳过 LLM 调用: {}", message.substring(0, Math.min(20, message.length())));
            return cached;
        }

        // 2. 未命中则调用 LLM
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);

        // 3. 缓存结果
        if (resultCache.shouldCache(content)) {
            resultCache.put(message, content);
        }
        return content;
    }

    /** SSE 流式对话 */
    public Flux<String> doChatByStream(String message, String chatId) {
        // 流式暂不做缓存（因为流式要逐 token 推送，命中缓存也得模拟流式）
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

    // ===== RAG 知识库问答（PGVector 旅游攻略知识库） =====

    @Resource
    @Qualifier("pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ResultCacheService resultCache2; // 为了 RAG 方法的缓存独立注入

    public String doChatWithRag(String message, String chatId) {
        // 1. 先查缓存
        String cached = resultCache2.get(message);
        if (cached != null) {
            log.info("RAG 缓存命中");
            return cached;
        }

        // 2. 查询重写 → PGVector 检索 → LLM 回答
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient.prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call().chatResponse();

        String content = chatResponse.getResult().getOutput().getText();

        // 3. 缓存结果
        if (resultCache2.shouldCache(content)) {
            resultCache2.put(message, content);
        }
        return content;
    }

    // ===== 工具调用（联网搜索景点、天气等） =====

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        // 工具调用结果多变，不做缓存
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
