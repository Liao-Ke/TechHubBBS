package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_part_detail")
public class FilePartDetail {
    @TableId(type = IdType.INPUT)
    private String id;

    private String platform;

    private String uploadId;

    private String eTag;

    private Integer partNumber;

    private Long partSize;

    private String hashInfo;

    private LocalDateTime createTime;
}
