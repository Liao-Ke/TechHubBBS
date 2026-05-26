package com.techhub.config;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传记录配置
 * x-file-storage FileRecorder 的占位实现
 */
@Configuration
public class FileRecorderConfig {

    @Bean
    public FileRecorder fileRecorder() {
        return new FileRecorder() {
            @Override
            public boolean save(FileInfo fileInfo) {
                return true;
            }

            @Override
            public void update(FileInfo fileInfo) {
                // no-op placeholder
            }

            @Override
            public FileInfo getByUrl(String url) {
                return null;
            }

            @Override
            public boolean delete(String url) {
                return true;
            }

            @Override
            public void saveFilePart(FilePartInfo filePartInfo) {
                // no-op placeholder
            }

            @Override
            public void deleteFilePartByUploadId(String uploadId) {
                // no-op placeholder
            }
        };
    }
}
