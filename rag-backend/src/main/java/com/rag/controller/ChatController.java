package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.dto.ChatMessageDTO;
import com.rag.entity.User;
import com.rag.repository.UserRepository;
import com.rag.service.PersistentChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天管理", description = "聊天问答相关接口")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final PersistentChatService chatService;
    private final UserRepository userRepository;

    public ChatController(PersistentChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
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
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        Long userId = getCurrentUserId();
        log.info("Processing chat request - sessionId: {}, question length: {}, userId: {}", 
                sessionId, request.getQuestion().length(), userId);
        
        ChatMessageDTO response = chatService.chat(sessionId, request.getQuestion(), request.getKnowledgeBaseId(), userId);
        return ApiResponse.success(response);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送消息", description = "向AI助手发送消息并以流式方式获取回答")
    public SseEmitter chatStream(
            @RequestParam String sessionId,
            @RequestParam String question,
            @RequestParam(required = false) Long knowledgeBaseId) {
        
        final String finalSessionId;
        if (sessionId == null || sessionId.isEmpty()) {
            finalSessionId = UUID.randomUUID().toString();
        } else {
            finalSessionId = sessionId;
        }
        
        log.info("Processing stream chat request - sessionId: {}, question length: {}", 
                finalSessionId, question.length());
        
        SseEmitter emitter = new SseEmitter(0L);
        
        final Long userId = getCurrentUserId();
        
        try {
            chatService.chatStream(
                finalSessionId, 
                question, 
                knowledgeBaseId, 
                userId,
                content -> {
                    try {
                        if (content != null && !content.isEmpty()) {
                            emitter.send(SseEmitter.event()
                                .name("message")
                                .data(content, MediaType.TEXT_PLAIN));
                        }
                    } catch (Exception e) {
                        log.error("Error sending SSE content", e);
                    }
                },
                error -> {
                    log.error("Stream error", error);
                    try {
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Error: " + error.getMessage()));
                    } catch (Exception e) {
                        log.error("Error sending error event", e);
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("Stream completed successfully - sessionId: {}", finalSessionId);
                    try {
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Error completing emitter", e);
                        try {
                            emitter.complete();
                        } catch (Exception ex) {
                            log.error("Error completing emitter (retry)", ex);
                        }
                    }
                    log.info("Emitter completed and connection closed - sessionId: {}", finalSessionId);
                }
            );
        } catch (Exception e) {
            log.error("Error in stream chat processing", e);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理失败：" + e.getMessage()));
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("Error sending error event", ex);
                emitter.completeWithError(e);
            }
        }
        
        emitter.onCompletion(() -> log.info("SSE completed - sessionId: {}", finalSessionId));
        emitter.onTimeout(() -> log.warn("SSE timeout - sessionId: {}", finalSessionId));
        emitter.onError(e -> log.error("SSE error - sessionId: {}", finalSessionId, e));
        
        return emitter;
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return 1L;
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
