package com.rag.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    @Test
    void testValidateFile_ValidPDF() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test content".getBytes()
        );
        
        assertDoesNotThrow(() -> FileValidator.validateFile(file));
    }

    @Test
    void testValidateFile_ValidDOCX() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "test content".getBytes()
        );
        
        assertDoesNotThrow(() -> FileValidator.validateFile(file));
    }

    @Test
    void testValidateFile_ValidImage() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            "test content".getBytes()
        );
        
        assertDoesNotThrow(() -> FileValidator.validateFile(file));
    }

    @Test
    void testValidateFile_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            new byte[0]
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(file)
        );
        assertEquals("文件不能为空", exception.getMessage());
    }

    @Test
    void testValidateFile_NullFile() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(null)
        );
        assertEquals("文件不能为空", exception.getMessage());
    }

    @Test
    void testValidateFile_InvalidExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.exe",
            "application/octet-stream",
            "test content".getBytes()
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(file)
        );
        assertTrue(exception.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void testValidateFile_NoExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "testfile",
            "application/octet-stream",
            "test content".getBytes()
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(file)
        );
        assertEquals("文件缺少扩展名", exception.getMessage());
    }

    @Test
    void testValidateFile_IllegalCharacters() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test<file>.pdf",
            "application/pdf",
            "test content".getBytes()
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(file)
        );
        assertEquals("文件名包含非法字符", exception.getMessage());
    }

    @Test
    void testGetFileExtension() {
        assertEquals("pdf", FileValidator.getFileExtension("test.pdf"));
        assertEquals("docx", FileValidator.getFileExtension("test.docx"));
        assertEquals("jpg", FileValidator.getFileExtension("test.jpg"));
        assertEquals("", FileValidator.getFileExtension("testfile"));
        assertEquals("", FileValidator.getFileExtension(null));
    }

    @Test
    void testGetFileExtension_UpperCase() {
        assertEquals("PDF", FileValidator.getFileExtension("test.PDF"));
        assertEquals("DOCX", FileValidator.getFileExtension("test.DOCX"));
    }

    @Test
    void testGetFileExtension_DotAtEnd() {
        assertEquals("", FileValidator.getFileExtension("test."));
    }

    @Test
    void testValidateFile_FileSizeTooLarge() {
        byte[] largeContent = new byte[501 * 1024 * 1024]; // 501MB
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            largeContent
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidator.validateFile(file)
        );
        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }

    @Test
    void testValidateFile_ValidSize() {
        byte[] validContent = new byte[100 * 1024 * 1024]; // 100MB
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            validContent
        );
        
        assertDoesNotThrow(() -> FileValidator.validateFile(file));
    }
}
