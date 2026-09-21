package com.kzip.app.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        // finance-api 调过来时做了 URL 编码，这里解码还原原文
        try {
            content = java.net.URLDecoder.decode(content, "UTF-8");
        } catch (Exception e) {
            // 解码失败就用原始 content
        }

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

    /**
     * 测试检索接口：直接看 Chroma 里检索到了哪些 chunk
     * GET /ai/cs/search?query=xxx&topK=5
     */
    @RequestMapping(value = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<List<Map<String, Object>>> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") int topK) {

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .build()
        );

        List<Map<String, Object>> result = docs.stream().map(doc -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", doc.getId());
            m.put("text", doc.getText().substring(0, Math.min(100, doc.getText().length())) + "...");
            m.put("metadata", doc.getMetadata());
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
