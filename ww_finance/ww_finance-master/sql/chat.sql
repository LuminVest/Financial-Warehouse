-- 智能客服聊天相关表

-- 1. 聊天会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT 0 COMMENT '用户ID（0表示未登录游客）',
  `user_name` VARCHAR(50) DEFAULT '' COMMENT '用户名',
  `user_phone` VARCHAR(20) DEFAULT '' COMMENT '用户手机号',
  `title` VARCHAR(200) DEFAULT '' COMMENT '会话标题（首条问题）',
  `kb_id` BIGINT DEFAULT 0 COMMENT '使用的知识库ID',
  `kb_name` VARCHAR(100) DEFAULT '' COMMENT '使用的知识库名称',
  `model` VARCHAR(100) DEFAULT 'qwen-plus' COMMENT '使用的模型',
  `message_count` INT DEFAULT 0 COMMENT '消息数',
  `status` TINYINT DEFAULT 0 COMMENT '0-进行中 1-已结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 2. 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user-用户 assistant-AI',
  `content` MEDIUMTEXT NOT NULL COMMENT '消息内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 3. Prompt 配置表
CREATE TABLE IF NOT EXISTS `chat_prompt_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `system_prompt` MEDIUMTEXT NOT NULL COMMENT '系统提示词',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
  `top_p` DECIMAL(3,2) DEFAULT 0.90 COMMENT 'Top P 参数',
  `max_tokens` INT DEFAULT 2000 COMMENT '最大生成 token 数',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
  `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
  `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 配置表';

-- 4. 模型配置表
CREATE TABLE IF NOT EXISTS `chat_model_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称（如 qwen-plus）',
  `base_url` VARCHAR(500) DEFAULT '' COMMENT 'API 地址',
  `api_key` VARCHAR(200) DEFAULT '' COMMENT 'API Key',
  `embedding_model` VARCHAR(100) DEFAULT 'text-embedding-v3' COMMENT '向量模型',
  `max_tokens` INT DEFAULT 4096 COMMENT '模型最大 token',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
  `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
  `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置表';

-- 插入默认 Prompt 配置
INSERT INTO `chat_prompt_config` (`name`, `system_prompt`, `temperature`, `top_p`, `max_tokens`, `is_default`, `status`, `remark`)
VALUES ('默认客服提示词', '你是旺旺金融的智能客服。请严格根据提供的知识库内容回答用户问题。如果知识库中没有相关信息，直接说"抱歉，这个问题我暂时回答不了"，绝对不要自己编造答案。回答要简洁、专业、有礼貌。', 0.70, 0.90, 2000, 1, 1, '系统默认配置');

-- 插入默认模型配置
INSERT INTO `chat_model_config` (`name`, `model_name`, `base_url`, `api_key`, `embedding_model`, `max_tokens`, `is_default`, `status`, `remark`)
VALUES ('通义千问默认配置', 'qwen-plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '', 'text-embedding-v3', 4096, 1, 1, '系统默认配置');
