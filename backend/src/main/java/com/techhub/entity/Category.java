package com.techhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String description;

    private Integer sortOrder;

    private Integer status;

    @TableField(exist = false)
    private Integer postCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
