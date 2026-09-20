package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.KnowledgeBase;
import com.wwfinance.api.entity.KnowledgeDoc;
import com.wwfinance.api.mapper.KnowledgeBaseMapper;
import com.wwfinance.api.mapper.KnowledgeDocMapper;
import com.wwfinance.api.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG知识库服务实现
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;

    @Override
    public List<KnowledgeBase> listAll() {
        return list(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getDeleted, false)
                .orderByDesc(KnowledgeBase::getUpdateTime));
    }

    @Override
    public void add(String name, String description, String embeddingModel) {
        KnowledgeBase kb = new KnowledgeBase()
                .setName(name)
                .setDescription(description == null ? "" : description)
                .setEmbeddingModel(embeddingModel == null ? "text-embedding-v3" : embeddingModel)
                .setDocCount(0)
                .setChunkCount(0)
                .setStatus(1);
        save(kb);
    }

    @Override
    public void update(Long id, String name, String description, String embeddingModel) {
        KnowledgeBase kb = getById(id);
        if (kb == null || Boolean.TRUE.equals(kb.getDeleted())) {
            throw new RuntimeException("知识库不存在");
        }
        kb.setName(name)
                .setDescription(description == null ? "" : description)
                .setEmbeddingModel(embeddingModel);
        updateById(kb);
    }

    @Override
    public void delete(Long id) {
        // 逻辑删除知识库
        KnowledgeBase kb = getById(id);
        if (kb == null) {
            return;
        }
        kb.setDeleted(true);
        updateById(kb);
        // 逻辑删除其下全部文档
        knowledgeDocMapper.update(null, new LambdaUpdateWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getKbId, id)
                .eq(KnowledgeDoc::getDeleted, false)
                .set(KnowledgeDoc::getDeleted, true));
    }
}
