package com.rag.service;

import com.rag.dto.KnowledgeBaseDTO;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBase testKnowledgeBase;
    private KnowledgeBaseDTO testKnowledgeBaseDTO;

    @BeforeEach
    void setUp() {
        testKnowledgeBase = new KnowledgeBase();
        testKnowledgeBase.setId(1L);
        testKnowledgeBase.setName("测试知识库");
        testKnowledgeBase.setDescription("这是一个测试知识库");
        testKnowledgeBase.setOwner("测试团队");
        testKnowledgeBase.setTags("测试,开发");
        testKnowledgeBase.setStatus("正常");

        testKnowledgeBaseDTO = new KnowledgeBaseDTO();
        testKnowledgeBaseDTO.setName("测试知识库");
        testKnowledgeBaseDTO.setDescription("这是一个测试知识库");
        testKnowledgeBaseDTO.setOwner("测试团队");
        testKnowledgeBaseDTO.setTags(Arrays.asList("测试", "开发"));
    }

    @Test
    void testGetAllKnowledgeBases() {
        when(knowledgeBaseRepository.findAll()).thenReturn(Arrays.asList(testKnowledgeBase));
        when(knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(1L)).thenReturn(10L);
        when(knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(1L)).thenReturn(100L);

        List<KnowledgeBaseDTO> result = knowledgeBaseService.getAllKnowledgeBases();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试知识库", result.get(0).getName());
        verify(knowledgeBaseRepository, times(1)).findAll();
    }

    @Test
    void testGetKnowledgeBaseById() {
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKnowledgeBase));
        when(knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(1L)).thenReturn(10L);
        when(knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(1L)).thenReturn(100L);

        KnowledgeBaseDTO result = knowledgeBaseService.getKnowledgeBaseById(1L);

        assertNotNull(result);
        assertEquals("测试知识库", result.getName());
        assertEquals(10L, result.getDocCount());
        assertEquals(100L, result.getVectorCount());
    }

    @Test
    void testGetKnowledgeBaseByIdNotFound() {
        when(knowledgeBaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            knowledgeBaseService.getKnowledgeBaseById(999L);
        });
    }

    @Test
    void testCreateKnowledgeBase() {
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenReturn(testKnowledgeBase);
        when(knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(1L)).thenReturn(0L);
        when(knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(1L)).thenReturn(0L);

        KnowledgeBaseDTO result = knowledgeBaseService.createKnowledgeBase(testKnowledgeBaseDTO);

        assertNotNull(result);
        assertEquals("测试知识库", result.getName());
        assertEquals("正常", result.getStatus());
        verify(knowledgeBaseRepository, times(1)).save(any(KnowledgeBase.class));
    }

    @Test
    void testUpdateKnowledgeBase() {
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(testKnowledgeBase));
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenReturn(testKnowledgeBase);
        when(knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(1L)).thenReturn(10L);
        when(knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(1L)).thenReturn(100L);

        KnowledgeBaseDTO updateDTO = new KnowledgeBaseDTO();
        updateDTO.setName("更新后的知识库");
        updateDTO.setDescription("更新后的描述");

        KnowledgeBaseDTO result = knowledgeBaseService.updateKnowledgeBase(1L, updateDTO);

        assertNotNull(result);
        assertEquals("更新后的知识库", result.getName());
        verify(knowledgeBaseRepository, times(1)).save(any(KnowledgeBase.class));
    }

    @Test
    void testDeleteKnowledgeBase() {
        when(knowledgeBaseRepository.existsById(1L)).thenReturn(true);
        doNothing().when(knowledgeBaseRepository).deleteById(1L);

        knowledgeBaseService.deleteKnowledgeBase(1L);

        verify(knowledgeBaseRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteKnowledgeBaseNotFound() {
        when(knowledgeBaseRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            knowledgeBaseService.deleteKnowledgeBase(999L);
        });
    }

    @Test
    void testSearchKnowledgeBases() {
        when(knowledgeBaseRepository.findByNameContaining("测试")).thenReturn(Arrays.asList(testKnowledgeBase));
        when(knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(1L)).thenReturn(10L);
        when(knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(1L)).thenReturn(100L);

        List<KnowledgeBaseDTO> result = knowledgeBaseService.searchKnowledgeBases("测试");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getName().contains("测试"));
    }
}
