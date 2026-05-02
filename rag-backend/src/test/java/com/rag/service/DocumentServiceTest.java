package com.rag.service;

import com.rag.dto.DocumentDTO;
import com.rag.entity.Document;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.DocumentRepository;
import com.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private TikaFileParserService fileParserService;

    @InjectMocks
    private DocumentService documentService;

    private Document testDocument;
    private KnowledgeBase testKnowledgeBase;

    @BeforeEach
    void setUp() {
        testKnowledgeBase = new KnowledgeBase();
        testKnowledgeBase.setId(1L);
        testKnowledgeBase.setName("测试知识库");

        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setName("测试文档.pdf");
        testDocument.setType("PDF");
        testDocument.setFileSize("2.5MB");
        testDocument.setStatus("已处理");
        testDocument.setKnowledgeBase(testKnowledgeBase);
        testDocument.setChunkCount(10);
        testDocument.setVectorCount(10);
    }

    @Test
    void testGetRecentDocuments() {
        when(documentRepository.findRecentDocuments(any(PageRequest.class))).thenReturn(Arrays.asList(testDocument));

        List<DocumentDTO> result = documentService.getRecentDocuments(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(documentRepository, times(1)).findRecentDocuments(any(PageRequest.class));
    }

    @Test
    void testGetDocumentsByKnowledgeBase() {
        when(documentRepository.findByKnowledgeBaseId(1L)).thenReturn(Arrays.asList(testDocument));

        List<DocumentDTO> result = documentService.getDocumentsByKnowledgeBase(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试文档.pdf", result.get(0).getName());
    }

    @Test
    void testDeleteDocument() {
        testDocument.setFilePath("tos://documents/test.pdf");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        doNothing().when(documentRepository).delete(testDocument);

        documentService.deleteDocument(1L);

        verify(documentRepository, times(1)).delete(testDocument);
    }

    @Test
    void testCountAllDocuments() {
        when(documentRepository.countAllDocuments()).thenReturn(100L);

        Long count = documentService.countAllDocuments();

        assertEquals(100L, count);
    }

    @Test
    void testSumAllVectorCount() {
        when(documentRepository.sumAllVectorCount()).thenReturn(1000L);

        Long count = documentService.sumAllVectorCount();

        assertEquals(1000L, count);
    }

    @Test
    void testSumAllVectorCountReturnsNull() {
        when(documentRepository.sumAllVectorCount()).thenReturn(null);

        Long count = documentService.sumAllVectorCount();

        assertEquals(0L, count);
    }

    @Test
    void testUpdateDocumentStatus() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        documentService.updateDocumentStatus(1L, "已处理");

        verify(documentRepository, times(1)).save(any(Document.class));
    }
}
