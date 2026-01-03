package com.gxj.cropyield.modules.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 简化版对象存储服务，默认写入本地磁盘，便于后续对接 MinIO/S3。
 */
@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);

    private final Path basePath;

    public ObjectStorageService(@Value("${object-store.base-path:./object-store}") String basePath) {
        this.basePath = Path.of(basePath);
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException ex) {
            log.warn("Unable to create object storage base directory {}", this.basePath, ex);
        }
    }

    public String saveText(String folder, String filenamePrefix, String content) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path targetFolder = basePath.resolve(folder);
        try {
            Files.createDirectories(targetFolder);
            Path targetFile = targetFolder.resolve(filenamePrefix + "-" + timestamp + ".json");
            Files.writeString(targetFile, content == null ? "" : content, StandardCharsets.UTF_8);
            return targetFile.toAbsolutePath().toString();
        } catch (IOException ex) {
            log.error("Failed to persist object to {}", targetFolder, ex);
            return null;
        }
    }
}
