package com.rag.service;

import com.rag.dto.ChatMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private RAGService ragService;

    @InjectMocks
    private ChatService chatService;

    private ChatMessageDTO testResponse;

    @BeforeEach
    void setUp() {
        testResponse = new ChatMessageDTO();
        testResponse.setId(1L);
        testResponse.setRole("assistant");
        testResponse.setContent("这是一个测试回答");
        testResponse.setTimestamp(java.time.LocalDateTime.now());
    }

    @Test
    void testChat() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        ChatMessageDTO result = chatService.chat("session_1", "测试问题", null);

        assertNotNull(result);
        assertEquals("assistant", result.getRole());
        assertNotNull(result.getContent());
        assertEquals("这是一个测试回答", result.getContent());
        verify(ragService, times(1)).answer("测试问题", null);
    }

    @Test
    void testChatWithKnowledgeBaseId() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        ChatMessageDTO result = chatService.chat("session_1", "测试问题", 1L);

        assertNotNull(result);
        assertEquals("assistant", result.getRole());
        verify(ragService, times(1)).answer("测试问题", 1L);
    }

    @Test
    void testChatHistory() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        chatService.chat("session_1", "问题1", null);
        chatService.chat("session_1", "问题2", null);

        List<ChatMessageDTO> history = chatService.getChatHistory("session_1");

        assertNotNull(history);
        assertEquals(4, history.size());
    }

    @Test
    void testGetChatHistoryEmpty() {
        List<ChatMessageDTO> history = chatService.getChatHistory("nonexistent_session");

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void testClearSession() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        chatService.chat("session_1", "测试问题", null);
        assertEquals(2, chatService.getChatHistory("session_1").size());

        chatService.clearSession("session_1");

        assertTrue(chatService.getChatHistory("session_1").isEmpty());
    }

    @Test
    void testChatErrorHandling() {
        when(ragService.answer(anyString(), any())).thenThrow(new RuntimeException("AI服务错误"));

        ChatMessageDTO result = chatService.chat("session_1", "测试问题", null);

        assertNotNull(result);
        assertEquals("assistant", result.getRole());
        assertTrue(result.getContent().contains("处理失败"));
        assertTrue(result.getContent().contains("AI服务错误"));
    }

    @Test
    void testMultipleSessions() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        chatService.chat("session_1", "问题1", null);
        chatService.chat("session_2", "问题2", null);

        List<ChatMessageDTO> history1 = chatService.getChatHistory("session_1");
        List<ChatMessageDTO> history2 = chatService.getChatHistory("session_2");

        assertEquals(2, history1.size());
        assertEquals(2, history2.size());
        assertNotEquals(history1, history2);
    }

    @Test
    void testActiveSessionCount() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        assertEquals(0, chatService.getActiveSessionCount());

        chatService.chat("session_1", "问题1", null);
        assertEquals(1, chatService.getActiveSessionCount());

        chatService.chat("session_2", "问题2", null);
        assertEquals(2, chatService.getActiveSessionCount());

        chatService.clearSession("session_1");
        assertEquals(1, chatService.getActiveSessionCount());
    }

    @Test
    void testChatWithNullKnowledgeBaseId() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        ChatMessageDTO result = chatService.chat("session_1", "测试问题", null);

        assertNotNull(result);
        verify(ragService, times(1)).answer("测试问题", null);
    }

    @Test
    void testMessageIdGeneration() {
        when(ragService.answer(anyString(), any())).thenReturn(testResponse);

        ChatMessageDTO result1 = chatService.chat("session_1", "问题1", null);
        ChatMessageDTO result2 = chatService.chat("session_1", "问题2", null);

        assertNotNull(result1.getId());
        assertNotNull(result2.getId());
        assertNotEquals(result1.getId(), result2.getId());
    }

    @Test
    void testSessionTimeoutConfiguration() {
        long timeout = chatService.getSessionTimeoutSeconds();
        assertTrue(timeout > 0);
        assertEquals(3600, timeout);
    }
}
