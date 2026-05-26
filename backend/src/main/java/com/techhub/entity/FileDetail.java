package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_detail")
public class FileDetail {
    @TableId(type = IdType.INPUT)
    private String id;

    private String url;

    private Long size;

    private String filename;

    private String originalFilename;

    private String basePath;

    private String path;

    private String ext;

    private String contentType;

    private String platform;

    private String thUrl;

    private String thFilename;

    private Long thSize;

    private String thContentType;

    private String objectId;

    private String objectType;

    private String metadata;

    private String userMetadata;

    private String thMetadata;

    private String thUserMetadata;

    private String attr;

    private String fileAcl;

    private String thFileAcl;

    private String hashInfo;

    private String uploadId;

    private Integer uploadStatus;

    private LocalDateTime createTime;
}
