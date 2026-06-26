package com.zhitu.travel.controller;

import com.zhitu.travel.agent.TravelPlannerAgent;
import com.zhitu.travel.app.TravelPlannerApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TravelPlannerApp travelPlannerApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /** 同步调用 */
    @GetMapping("/travel/chat/sync")
    public String doChatSync(String message, String chatId) {
        return travelPlannerApp.doChat(message, chatId);
    }

    /** SSE 流式调用 */
    @GetMapping(value = "/travel/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatSSE(String message, String chatId) {
        return travelPlannerApp.doChatByStream(message, chatId);
    }

    /** SSE ServerSentEvent 格式 */
    @GetMapping(value = "/travel/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatServerSentEvent(String message, String chatId) {
        return travelPlannerApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    /** SseEmitter 流式调用 */
    @GetMapping(value = "/travel/chat/sse_emitter")
    public SseEmitter doChatSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        travelPlannerApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try { sseEmitter.send(chunk); }
                    catch (IOException e) { sseEmitter.completeWithError(e); }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    /** 流式调用 TravelPlanner 智能体 */
    @GetMapping("/travel/agent/chat")
    public SseEmitter doChatWithAgent(String message) {
        TravelPlannerAgent agent = new TravelPlannerAgent(allTools, dashscopeChatModel);
        return agent.runStream(message);
    }
}
