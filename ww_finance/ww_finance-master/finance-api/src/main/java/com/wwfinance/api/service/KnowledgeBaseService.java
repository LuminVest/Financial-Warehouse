package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.KnowledgeBase;

import java.util.List;

/**
 * RAG知识库服务
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /** 全部知识库（未删除，按更新时间倒序） */
    List<KnowledgeBase> listAll();

    /** 新增 */
    void add(String name, String description, String embeddingModel);

    /** 修改 */
    void update(Long id, String name, String description, String embeddingModel);

    /** 删除（逻辑删除，连带删除其下文档） */
    void delete(Long id);
}
