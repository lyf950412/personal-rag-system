package com.rag.service;

import com.rag.dto.ChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RAGService ragService;
    private final ConcurrentHashMap<String, List<ChatMessageDTO>> sessionHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    private final AtomicLong messageIdCounter = new AtomicLong(1);

    @Value("${ai.chat.session-timeout:3600}")
    private long sessionTimeoutSeconds;

    @Value("${ai.chat.max-history-size:100}")
    private int maxHistorySize;

    public ChatService(DocumentService documentService, KnowledgeBaseService knowledgeBaseService, RAGService ragService) {
        this.documentService = documentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragService = ragService;
    }

    public List<ChatMessageDTO> getChatHistory(String sessionId) {
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    public ChatMessageDTO chat(String sessionId, String question, Long knowledgeBaseId) {
        log.info("Processing chat request - session: {}, question: {}, KB: {}", sessionId, question, knowledgeBaseId);

        List<ChatMessageDTO> history = sessionHistory.computeIfAbsent(sessionId, k -> {
            sessionLastActivity.put(sessionId, LocalDateTime.now());
            return new ArrayList<>();
        });
        
        ChatMessageDTO userMessage = createUserMessage(question);
        history.add(userMessage);
        sessionLastActivity.put(sessionId, LocalDateTime.now());

        if (history.size() > maxHistorySize) {
            int removeCount = history.size() - maxHistorySize;
            for (int i = 0; i < removeCount; i++) {
                history.remove(0);
            }
            log.info("Trimmed {} old messages from session {}", removeCount, sessionId);
        }

        try {
            ChatMessageDTO response = ragService.answer(question, knowledgeBaseId);
            response.setId(messageIdCounter.getAndIncrement());
            history.add(response);
            sessionLastActivity.put(sessionId, LocalDateTime.now());

            log.info("Chat completed successfully - response length: {}", response.getContent().length());
            return response;
        } catch (Exception e) {
            log.error("Error in chat processing", e);
            ChatMessageDTO errorResponse = createErrorResponse("处理失败：" + e.getMessage());
            history.add(errorResponse);
            sessionLastActivity.put(sessionId, LocalDateTime.now());
            return errorResponse;
        }
    }

    public Flux<String> chatStream(String sessionId, String question, Long knowledgeBaseId, Long userId) {
        log.info("Processing chat stream request - session: {}, question: {}, KB: {}, userId: {}", sessionId, question, knowledgeBaseId, userId);

        List<ChatMessageDTO> history = sessionHistory.computeIfAbsent(sessionId, k -> {
            sessionLastActivity.put(sessionId, LocalDateTime.now());
            return new ArrayList<>();
        });
        
        ChatMessageDTO userMessage = createUserMessage(question);
        history.add(userMessage);
        sessionLastActivity.put(sessionId, LocalDateTime.now());

        if (history.size() > maxHistorySize) {
            int removeCount = history.size() - maxHistorySize;
            for (int i = 0; i < removeCount; i++) {
                history.remove(0);
            }
            log.info("Trimmed {} old messages from session {}", removeCount, sessionId);
        }

        Flux<String> stream = ragService.answerStream(question, knowledgeBaseId);
        
        stream.subscribe(
            null,
            null,
            () -> {
                ChatMessageDTO assistantMessage = createAssistantMessage("");
                history.add(assistantMessage);
                sessionLastActivity.put(sessionId, LocalDateTime.now());
                log.info("Chat stream completed - session: {}", sessionId);
            }
        );

        return stream;
    }

    private ChatMessageDTO createUserMessage(String content) {
        ChatMessageDTO message = new ChatMessageDTO();
        message.setId(messageIdCounter.getAndIncrement());
        message.setRole("user");
        message.setContent(content);
        message.setTimestamp(java.time.LocalDateTime.now());
        return message;
    }

    private ChatMessageDTO createAssistantMessage(String content) {
        ChatMessageDTO message = new ChatMessageDTO();
        message.setId(messageIdCounter.getAndIncrement());
        message.setRole("assistant");
        message.setContent(content);
        message.setTimestamp(java.time.LocalDateTime.now());
        return message;
    }

    private ChatMessageDTO createErrorResponse(String errorMessage) {
        ChatMessageDTO message = new ChatMessageDTO();
        message.setId(messageIdCounter.getAndIncrement());
        message.setRole("assistant");
        message.setContent("抱歉，处理您的请求时遇到问题：" + errorMessage);
        message.setTimestamp(java.time.LocalDateTime.now());
        message.setSources(new ArrayList<>());
        return message;
    }

    public void clearSession(String sessionId) {
        log.info("Clearing chat session - sessionId: {}", sessionId);
        sessionHistory.remove(sessionId);
        sessionLastActivity.remove(sessionId);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        log.info("Starting expired session cleanup...");
        LocalDateTime expiryTime = LocalDateTime.now().minusSeconds(sessionTimeoutSeconds);
        int beforeCount = sessionHistory.size();

        sessionLastActivity.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(expiryTime)) {
                sessionHistory.remove(entry.getKey());
                log.debug("Removed expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });

        int removedCount = beforeCount - sessionHistory.size();
        log.info("Session cleanup completed. Removed {} expired sessions. Active sessions: {}", 
                removedCount, sessionHistory.size());
    }

    public int getActiveSessionCount() {
        return sessionHistory.size();
    }

    public long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }
}
