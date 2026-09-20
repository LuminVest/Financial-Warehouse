package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.KnowledgeDoc;

import java.util.List;

/**
 * 知识库文档服务
 */
public interface KnowledgeDocService extends IService<KnowledgeDoc> {

    /** 按知识库查询文档列表（未删除，按时间正序） */
    List<KnowledgeDoc> listByKbId(Long kbId);

    /** 新增文本文档 */
    void addText(Long kbId, String title, String content);

    /** 新增 PDF 文档（先存元数据，文件解析/向量化后续由 AI 服务完成） */
    void addPdf(Long kbId, String title, String fileName, Long fileSize);

    /** 删除文档（并回退所属知识库的计数） */
    void delete(Long id);
}
