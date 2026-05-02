package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.dto.ChatMessageDTO;
import com.rag.service.PersistentChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天管理", description = "聊天问答相关接口")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final PersistentChatService chatService;

    public ChatController(PersistentChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取聊天历史", description = "根据会话ID获取该会话的所有聊天记录")
    @Parameter(name = "sessionId", description = "会话ID", required = true)
    public ApiResponse<List<ChatMessageDTO>> getChatHistory(@PathVariable String sessionId) {
        log.debug("Getting chat history for session: {}", sessionId);
        List<ChatMessageDTO> history = chatService.getChatHistory(sessionId);
        return ApiResponse.success(history);
    }

    @PostMapping
    @Operation(summary = "发送消息", description = "向AI助手发送消息并获取回答")
    public ApiResponse<ChatMessageDTO> chat(@Valid @RequestBody ChatRequest request) {
        Long userId = chatService.getCurrentUserId();
        log.info("Processing chat request - sessionId: {}, question length: {}, userId: {}", 
                request.getSessionId(), request.getQuestion().length(), userId);
        
        ChatMessageDTO response = chatService.chat(
                request.getSessionId(), 
                request.getQuestion(), 
                request.getKnowledgeBaseId(), 
                userId);
        return ApiResponse.success(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送消息", description = "向AI助手发送消息并以流式方式获取回答")
    public SseEmitter chatStream(
            @RequestParam String sessionId,
            @RequestParam String question,
            @RequestParam(required = false) Long knowledgeBaseId) {
        
        Long userId = chatService.getCurrentUserId();
        return chatService.chatStream(sessionId, question, knowledgeBaseId, userId);
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "清除会话", description = "清除指定会话的所有聊天记录")
    @Parameter(name = "sessionId", description = "会话ID", required = true)
    public ApiResponse<Void> clearSession(@PathVariable String sessionId) {
        log.info("Clearing chat session: {}", sessionId);
        chatService.clearSession(sessionId);
        return ApiResponse.success("会话已清空", null);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取会话统计", description = "获取当前活跃会话数量和配置信息")
    public ApiResponse<Object> getChatStats() {
        return ApiResponse.success(java.util.Map.of(
                "activeSessions", chatService.getActiveSessionCount(),
                "sessionTimeout", chatService.getSessionTimeoutSeconds()
        ));
    }

    public static class ChatRequest {
        private String sessionId;
        @jakarta.validation.constraints.NotBlank(message = "问题不能为空")
        @jakarta.validation.constraints.Size(max = 2000, message = "问题长度不能超过2000字符")
        private String question;
        private Long knowledgeBaseId;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public Long getKnowledgeBaseId() { return knowledgeBaseId; }
        public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    }
}
