package com.techhub.config;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.entity.FileDetail;
import com.techhub.mapper.FileDetailMapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传记录配置
 * 使用 MyBatis-Plus + file_detail 表持久化文件上传记录
 */
@Configuration
public class FileRecorderConfig {

    @Bean
    public FileRecorder fileRecorder(FileDetailMapper fileDetailMapper, ObjectMapper objectMapper) {
        return new FileRecorder() {
            @Override
            public boolean save(FileInfo fileInfo) {
                FileDetail detail = toFileDetail(fileInfo, objectMapper);
                return fileDetailMapper.insert(detail) > 0;
            }

            @Override
            public void update(FileInfo fileInfo) {
                FileDetail detail = toFileDetail(fileInfo, objectMapper);
                fileDetailMapper.updateById(detail);
            }

            @Override
            public FileInfo getByUrl(String url) {
                FileDetail detail = fileDetailMapper.selectOne(
                        new LambdaQueryWrapper<FileDetail>().eq(FileDetail::getUrl, url));
                return detail != null ? toFileInfo(detail, objectMapper) : null;
            }

            @Override
            public boolean delete(String url) {
                return fileDetailMapper.delete(
                        new LambdaQueryWrapper<FileDetail>().eq(FileDetail::getUrl, url)) > 0;
            }

            @Override
            public void saveFilePart(FilePartInfo filePartInfo) {
                // 分片上传暂不需要实现
            }

            @Override
            public void deleteFilePartByUploadId(String uploadId) {
                // 分片上传暂不需要实现
            }

            private FileDetail toFileDetail(FileInfo info, ObjectMapper om) {
                FileDetail detail = new FileDetail();
                detail.setId(info.getId() != null && !info.getId().isEmpty()
                        ? info.getId()
                        : UUID.randomUUID().toString().replace("-", ""));
                detail.setUrl(info.getUrl());
                detail.setSize(info.getSize());
                detail.setFilename(info.getFilename());
                detail.setOriginalFilename(info.getOriginalFilename());
                detail.setBasePath(info.getBasePath());
                detail.setPath(info.getPath());
                detail.setExt(info.getExt());
                detail.setContentType(info.getContentType());
                detail.setPlatform(info.getPlatform());
                detail.setThUrl(info.getThUrl());
                detail.setThFilename(info.getThFilename());
                detail.setThSize(info.getThSize());
                detail.setThContentType(info.getThContentType());
                detail.setObjectId(info.getObjectId());
                detail.setObjectType(info.getObjectType());
                detail.setMetadata(toJsonString(info.getMetadata(), om));
                detail.setUserMetadata(toJsonString(info.getUserMetadata(), om));
                detail.setThMetadata(toJsonString(info.getThMetadata(), om));
                detail.setThUserMetadata(toJsonString(info.getThUserMetadata(), om));
                detail.setAttr(toJsonString(info.getAttr(), om));
                detail.setFileAcl(toJsonString(info.getFileAcl(), om));
                detail.setThFileAcl(toJsonString(info.getThFileAcl(), om));
                detail.setHashInfo(toJsonString(info.getHashInfo(), om));
                detail.setUploadId(info.getUploadId());
                detail.setUploadStatus(info.getUploadStatus());
                detail.setCreateTime(info.getCreateTime() != null
                        ? LocalDateTime.ofInstant(info.getCreateTime().toInstant(), ZoneId.systemDefault())
                        : LocalDateTime.now());
                return detail;
            }

            private FileInfo toFileInfo(FileDetail detail, ObjectMapper om) {
                FileInfo info = new FileInfo();
                info.setId(detail.getId());
                info.setUrl(detail.getUrl());
                info.setSize(detail.getSize());
                info.setFilename(detail.getFilename());
                info.setOriginalFilename(detail.getOriginalFilename());
                info.setBasePath(detail.getBasePath());
                info.setPath(detail.getPath());
                info.setExt(detail.getExt());
                info.setContentType(detail.getContentType());
                info.setPlatform(detail.getPlatform());
                info.setThUrl(detail.getThUrl());
                info.setThFilename(detail.getThFilename());
                info.setThSize(detail.getThSize());
                info.setThContentType(detail.getThContentType());
                info.setObjectId(detail.getObjectId());
                info.setObjectType(detail.getObjectType());
                info.setMetadata(fromJsonString(detail.getMetadata(), new TypeReference<Map<String, String>>() {}, om));
                info.setUserMetadata(fromJsonString(detail.getUserMetadata(), new TypeReference<Map<String, String>>() {}, om));
                info.setThMetadata(fromJsonString(detail.getThMetadata(), new TypeReference<Map<String, String>>() {}, om));
                info.setThUserMetadata(fromJsonString(detail.getThUserMetadata(), new TypeReference<Map<String, String>>() {}, om));
                info.setAttr(fromJsonString(detail.getAttr(), new TypeReference<Dict>() {}, om));
                info.setFileAcl(detail.getFileAcl());
                info.setThFileAcl(detail.getThFileAcl());
                info.setHashInfo(fromJsonString(detail.getHashInfo(), new TypeReference<HashInfo>() {}, om));
                info.setUploadId(detail.getUploadId());
                info.setUploadStatus(detail.getUploadStatus());
                info.setCreateTime(detail.getCreateTime() != null
                        ? Date.from(detail.getCreateTime().atZone(ZoneId.systemDefault()).toInstant())
                        : new Date());
                return info;
            }

            private String toJsonString(Object obj, ObjectMapper om) {
                if (obj == null) return null;
                try {
                    return om.writeValueAsString(obj);
                } catch (Exception e) {
                    return null;
                }
            }

            private <T> T fromJsonString(String json, TypeReference<T> typeRef, ObjectMapper om) {
                if (json == null || json.isEmpty()) return null;
                try {
                    return om.readValue(json, typeRef);
                } catch (Exception e) {
                    return null;
                }
            }

        };
    }
}
