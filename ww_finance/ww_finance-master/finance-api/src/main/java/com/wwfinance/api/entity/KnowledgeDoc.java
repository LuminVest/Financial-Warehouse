package com.wwfinance.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("knowledge_doc")
public class KnowledgeDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("kb_id")
    private Long kbId;

    @TableField("title")
    private String title;

    @TableField("source")
    private String source;

    @TableField("content")
    private String content;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("is_deleted")
    private Boolean deleted;
}
