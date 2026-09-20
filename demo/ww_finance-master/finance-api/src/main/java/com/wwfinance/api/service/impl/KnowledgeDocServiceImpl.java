package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.KnowledgeBase;
import com.wwfinance.api.entity.KnowledgeDoc;
import com.wwfinance.api.mapper.KnowledgeBaseMapper;
import com.wwfinance.api.mapper.KnowledgeDocMapper;
import com.wwfinance.api.service.KnowledgeDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库文档服务实现
 */
@Service
public class KnowledgeDocServiceImpl extends ServiceImpl<KnowledgeDocMapper, KnowledgeDoc> implements KnowledgeDocService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public List<KnowledgeDoc> listByKbId(Long kbId) {
        return list(new LambdaQueryWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getKbId, kbId)
                .eq(KnowledgeDoc::getDeleted, false)
                .orderByAsc(KnowledgeDoc::getId));
    }

    @Override
    public void addText(Long kbId, String title, String content) {
        int chunkCount = (int) Math.ceil(content.length() / 500.0);
        KnowledgeDoc doc = new KnowledgeDoc()
                .setKbId(kbId)
                .setTitle(title)
                .setSource("text")
                .setContent(content)
                .setFileName("")
                .setFileSize(0L)
                .setChunkCount(chunkCount)
                .setStatus(1);
        save(doc);
        bumpCount(kbId, 1, chunkCount);
    }

    @Override
    public void addPdf(Long kbId, String title, String fileName, Long fileSize) {
        int chunkCount = (int) Math.ceil(fileSize / 50.0);
        KnowledgeDoc doc = new KnowledgeDoc()
                .setKbId(kbId)
                .setTitle(title)
                .setSource("pdf")
                .setContent("")
                .setFileName(fileName)
                .setFileSize(fileSize)
                .setChunkCount(chunkCount)
                .setStatus(0); // 处理中（待 AI 服务解析向量化）
        save(doc);
        bumpCount(kbId, 1, chunkCount);
    }

    @Override
    public void delete(Long id) {
        KnowledgeDoc doc = getById(id);
        if (doc == null || Boolean.TRUE.equals(doc.getDeleted())) {
            return;
        }
        doc.setDeleted(true);
        updateById(doc);
        // 回退所属知识库计数
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());
        if (kb != null) {
            kb.setDocCount(Math.max(0, kb.getDocCount() - 1));
            kb.setChunkCount(Math.max(0, kb.getChunkCount() - doc.getChunkCount()));
            knowledgeBaseMapper.updateById(kb);
        }
    }

    /** 知识库文档数/分块数累加 */
    private void bumpCount(Long kbId, int docDelta, int chunkDelta) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb != null) {
            kb.setDocCount(kb.getDocCount() + docDelta);
            kb.setChunkCount(kb.getChunkCount() + chunkDelta);
            knowledgeBaseMapper.updateById(kb);
        }
    }
}
