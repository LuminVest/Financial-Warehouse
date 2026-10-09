package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chat_prompt_config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatPromptConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String systemPrompt;

    private BigDecimal temperature;

    private BigDecimal topP;

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
