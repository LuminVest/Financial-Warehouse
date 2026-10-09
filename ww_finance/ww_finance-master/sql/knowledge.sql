-- RAG 知识库管理表
-- 对应前端 ww_finance_admin 的知识库列表/文档管理

-- 知识库（元数据）
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '描述',
  `doc_count` INT NOT NULL DEFAULT 0 COMMENT '文档数',
  `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '分块数',
  `embedding_model` VARCHAR(100) NOT NULL DEFAULT 'text-embedding-v3' COMMENT '向量模型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-未启用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库';

-- 知识库文档
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `source` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '来源: text/pdf/url',
  `content` MEDIUMTEXT COMMENT '文本内容(text类型原文)',
  `file_name` VARCHAR(200) DEFAULT '' COMMENT '文件名',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(KB)',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数',
  `status` TINYINT DEFAULT 1 COMMENT '0-处理中 1-就绪 2-失败',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';
