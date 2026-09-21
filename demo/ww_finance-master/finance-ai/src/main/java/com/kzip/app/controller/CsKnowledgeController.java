package com.kzip.app.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档入库接口：
 *   把管理端传过来的文本切块、向量化后写入 ChromaDB。
 *   后续 /ai/chat 会通过 QuestionAnswerAdvisor 从这里检索片段拼给大模型。
 *
 *   POST /ai/cs/knowledge?content=xxx&docId=xxx&type=target
 */
@RestController
@RequestMapping("/ai/cs")
public class CsKnowledgeController {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public CsKnowledgeController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // TokenTextSplitter 默认按 token 切，块大小 800、重叠 100，适合中文知识库
        this.textSplitter = new TokenTextSplitter();
    }

    /**
     * 文本入库：content 为原文，docId 对应 knowledge_doc 主键，type 区分业务类型（如 target/faq）
     * 同时支持 GET（浏览器方便测试）和 POST（正式前端调用）
     */
    @RequestMapping(value = "/knowledge", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Map<String, Object>> addKnowledge(
            @RequestParam String content,
            @RequestParam(required = false) String docId,
            @RequestParam(required = false, defaultValue = "default") String type) {

        // 1. 原始文本包装成 Document
        Map<String, Object> metadata = new HashMap<>();
        if (docId != null) {
            metadata.put("docId", docId);
        }
        metadata.put("type", type);
        metadata.put("source", "knowledge");

        Document original = new Document(content, metadata);

        // 2. TokenTextSplitter 切块
        List<Document> chunks = textSplitter.apply(List.of(original));

        // 3. 写入 ChromaDB（内部自动调 EmbeddingModel 向量化）
        vectorStore.add(chunks);

        Map<String, Object> data = new HashMap<>();
        data.put("chunkCount", chunks.size());
        data.put("type", type);
        return Result.success(data);
    }
}
