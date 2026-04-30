package com.rag.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;
import java.util.regex.Pattern;

public class FileValidator {
    
    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "PDF", "DOCX", "DOC", "TXT", "MD", "MARKDOWN",
        "XLSX", "XLS", "CSV",
        "PNG", "JPG", "JPEG", "GIF", "WEBP",
        "MP3", "WAV", "AAC",
        "MP4", "AVI", "MOV"
    );
    
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}._-]+$");
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024L;
    
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        validateFileName(file.getOriginalFilename());
        validateFileSize(file);
        validateFileExtension(file);
    }
    
    private static void validateFileName(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (filename.length() > 255) {
            throw new IllegalArgumentException("文件名长度不能超过255个字符");
        }
        if (!SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
            throw new IllegalArgumentException("文件名包含非法字符");
        }
    }
    
    private static void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大500MB）");
        }
        if (file.getSize() == 0) {
            throw new IllegalArgumentException("文件不能为空");
        }
    }
    
    private static void validateFileExtension(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename()).toUpperCase();
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
    }
    
    public static String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).trim();
    }
}
