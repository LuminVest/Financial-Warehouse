package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_model_config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatModelConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String modelName;

    private String baseUrl;

    private String apiKey;

    private String embeddingModel;

    private Integer maxTokens;

    private Integer isDefault;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
