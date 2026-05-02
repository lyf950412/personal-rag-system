package com.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.dto.ChatMessageDTO;
import com.rag.entity.ChatMessage;
import com.rag.entity.ChatSession;
import com.rag.entity.User;
import com.rag.repository.ChatMessageRepository;
import com.rag.repository.ChatSessionRepository;
import com.rag.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PersistentChatService {

    private static final Logger log = LoggerFactory.getLogger(PersistentChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RAGService ragService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Value("${ai.chat.max-history-size:100}")
    private int maxHistorySize;

    @Value("${ai.chat.session-timeout:86400}")
    private long sessionTimeoutSeconds;

    public PersistentChatService(ChatSessionRepository sessionRepository,
                                 ChatMessageRepository messageRepository,
                                 RAGService ragService,
                                 ObjectMapper objectMapper,
                                 UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.ragService = ragService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChatHistory(String sessionId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (session == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        
        return messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDTO chat(String sessionId, String question, Long knowledgeBaseId, Long userId) {
        String finalSessionId = sessionId != null && !sessionId.isEmpty() ? sessionId : java.util.UUID.randomUUID().toString();
        
        log.info("Processing chat request - session: {}, question: {}, KB: {}, user: {}", 
                finalSessionId, question, knowledgeBaseId, userId);

        ChatSession session = sessionRepository.findBySessionId(finalSessionId)
                .orElseGet(() -> createSession(finalSessionId, userId, knowledgeBaseId));

        ChatMessage userMessage = createAndSaveMessage(session, "user", question);

        try {
            ChatMessageDTO responseDTO = ragService.answer(question, knowledgeBaseId);
            
            String sourcesJson = null;
            if (responseDTO.getSources() != null && !responseDTO.getSources().isEmpty()) {
                try {
                    sourcesJson = objectMapper.writeValueAsString(responseDTO.getSources());
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize sources", e);
                }
            }

            ChatMessage assistantMessage = ChatMessage.builder()
                    .session(session)
                    .role("assistant")
                    .content(responseDTO.getContent())
                    .sources(sourcesJson)
                    .build();
            
            assistantMessage = messageRepository.save(assistantMessage);
            session.addMessage(assistantMessage);
            sessionRepository.save(session);

            trimHistoryIfNeeded(session);

            log.info("Chat completed successfully - response length: {}", responseDTO.getContent().length());
            
            return toDTO(assistantMessage);
            
        } catch (Exception e) {
            log.error("Error in chat processing", e);
            ChatMessage errorMessage = createAndSaveMessage(session, "assistant", 
                    "抱歉，处理您的请求时遇到问题：" + e.getMessage());
            return toDTO(errorMessage);
        }
    }

    public SseEmitter chatStream(String sessionId, String question, Long knowledgeBaseId, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        
        final String finalSessionId = sessionId != null && !sessionId.isEmpty() ? sessionId : java.util.UUID.randomUUID().toString();
        
        log.info("Processing stream chat request - sessionId: {}, question length: {}, userId: {}", 
                finalSessionId, question.length(), userId);
        
        ChatSession session = sessionRepository.findBySessionId(finalSessionId)
                .orElseGet(() -> createSession(finalSessionId, userId, knowledgeBaseId));

        createAndSaveMessage(session, "user", question);

        Flux<String> stream = ragService.answerStream(question, knowledgeBaseId);
        
        final StringBuilder fullResponse = new StringBuilder();
        
        stream.subscribe(
            content -> {
                fullResponse.append(content);
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(content, MediaType.TEXT_PLAIN));
                } catch (IOException e) {
                    log.error("Error sending SSE content", e);
                }
            },
            error -> {
                log.error("Stream error in session {}", finalSessionId, error);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Error: " + error.getMessage()));
                } catch (IOException e) {
                    log.error("Error sending error event", e);
                }
                emitter.completeWithError(error);
            },
            () -> {
                log.info("Stream completed for session {}, response length: {}", finalSessionId, fullResponse.length());
                createAndSaveMessage(session, "assistant", fullResponse.toString());
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data("[DONE]"));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Error completing emitter", e);
                    try {
                        emitter.complete();
                    } catch (IOException ex) {
                        log.error("Error completing emitter (retry)", ex);
                    }
                }
                log.info("Emitter completed and connection closed - sessionId: {}", finalSessionId);
            }
        );
        
        emitter.onCompletion(() -> log.info("SSE completed - sessionId: {}", finalSessionId));
        emitter.onTimeout(() -> log.warn("SSE timeout - sessionId: {}", finalSessionId));
        emitter.onError(e -> log.error("SSE error - sessionId: {}", finalSessionId, e));
        
        return emitter;
    }

    @Transactional
    public void clearSession(String sessionId) {
        log.info("Clearing chat session - sessionId: {}", sessionId);
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElse(null);
        
        if (session != null) {
            messageRepository.deleteBySessionId(session.getId());
            sessionRepository.delete(session);
            log.info("Session cleared successfully");
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting expired session cleanup...");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusSeconds(sessionTimeoutSeconds);
        List<ChatSession> expiredSessions = sessionRepository.findExpiredSessions(expiryTime);
        
        for (ChatSession session : expiredSessions) {
            messageRepository.deleteBySessionId(session.getId());
            sessionRepository.delete(session);
            log.debug("Removed expired session: {}", session.getSessionId());
        }

        log.info("Session cleanup completed. Removed {} expired sessions.", expiredSessions.size());
    }

    public int getActiveSessionCount() {
        return (int) sessionRepository.count();
    }

    public long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public Long getCurrentUserId() {
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

    private ChatSession createSession(String sessionId, Long userId, Long knowledgeBaseId) {
        log.info("Creating new chat session - sessionId: {}, userId: {}, kbId: {}", sessionId, userId, knowledgeBaseId);
        
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .knowledgeBaseId(knowledgeBaseId)
                .build();
        
        return sessionRepository.save(session);
    }

    private ChatMessage createAndSaveMessage(ChatSession session, String role, String content) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .build();
        
        message = messageRepository.save(message);
        session.addMessage(message);
        sessionRepository.save(session);
        
        return message;
    }

    private void trimHistoryIfNeeded(ChatSession session) {
        long messageCount = messageRepository.countBySessionId(session.getId());
        
        if (messageCount > maxHistorySize) {
            List<ChatMessage> messages = messageRepository.findRecentMessagesBySessionId(
                    session.getId(), (int) (messageCount - maxHistorySize));
            
            if (!messages.isEmpty()) {
                messageRepository.deleteAll(messages);
                log.info("Trimmed {} old messages from session {}", messages.size(), session.getSessionId());
            }
        }
    }

    private ChatMessageDTO toDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getCreatedAt());

        if (message.getSources() != null) {
            try {
                List<ChatMessageDTO.SourceDTO> sources = objectMapper.readValue(
                        message.getSources(),
                        new TypeReference<List<ChatMessageDTO.SourceDTO>>() {}
                );
                dto.setSources(sources);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize sources", e);
            }
        }

        return dto;
    }
}
