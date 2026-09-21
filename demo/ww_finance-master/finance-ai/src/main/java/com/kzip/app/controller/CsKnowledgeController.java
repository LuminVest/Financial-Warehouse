package com.kzip.app.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
     * 上传 PDF 文件入库：
     * POST /ai/cs/knowledge/pdf
     * 参数：file=PDF文件, docId=文档ID, type=文档类型
     */
    @RequestMapping(value = "/knowledge/pdf", method = {RequestMethod.POST})
    public Result<Map<String, Object>> addPdfKnowledge(
            @RequestParam("file") MultipartFile file,
            @RequestParam String docId,
            @RequestParam(required = false, defaultValue = "default") String type) {

        String financeApiBase = "http://localhost:8990";
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 1. 用 PagePdfDocumentReader 读取 PDF
            InputStream inputStream = file.getInputStream();
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(inputStream));
            List<Document> originalDocs = pdfReader.get();

            // 2. 加元数据
            for (Document doc : originalDocs) {
                doc.getMetadata().put("docId", docId);
                doc.getMetadata().put("type", type);
                doc.getMetadata().put("source", "knowledge");
                doc.getMetadata().put("fileName", file.getOriginalFilename());
            }

            // 3. TokenTextSplitter 切块
            List<Document> chunks = textSplitter.apply(originalDocs);

            // 4. 写入 ChromaDB
            vectorStore.add(chunks);

            // 5. 把 PDF 全文拼起来
            StringBuilder fullText = new StringBuilder();
            for (Document doc : originalDocs) {
                fullText.append(doc.getText()).append("\n\n");
            }
            String content = fullText.toString().trim();

            // 6. 回调 finance-api 更新状态为就绪（status=1），同时存全文
            try {
                String callbackUrl = financeApiBase + "/admin/core/knowledge/doc/updateStatus";
                Map<String, Object> callbackBody = new HashMap<>();
                callbackBody.put("docId", docId);
                callbackBody.put("status", 1);
                callbackBody.put("chunkCount", chunks.size());
                callbackBody.put("content", content);
                restTemplate.postForObject(callbackUrl, callbackBody, String.class);
                System.out.println("已回调 finance-api 更新文档状态为就绪, docId=" + docId);
            } catch (Exception callbackEx) {
                System.err.println("回调 finance-api 更新状态失败: " + callbackEx.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("chunkCount", chunks.size());
            data.put("pageCount", originalDocs.size());
            data.put("type", type);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();

            // 失败也回调，更新状态为失败（status=2）
            try {
                String callbackUrl = financeApiBase + "/admin/core/knowledge/doc/updateStatus";
                Map<String, Object> callbackBody = new HashMap<>();
                callbackBody.put("docId", docId);
                callbackBody.put("status", 2);
                restTemplate.postForObject(callbackUrl, callbackBody, String.class);
            } catch (Exception callbackEx) {
                System.err.println("回调 finance-api 更新失败状态也失败: " + callbackEx.getMessage());
            }

            return Result.error("PDF 解析失败: " + e.getMessage());
        }
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

    /**
     * 按 docId 删除知识库向量
     * GET /ai/cs/delete?docId=xxx
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Map<String, Object>> deleteByDocId(@RequestParam String docId) {
        try {
            // 直接调 Chroma REST API，按 where 条件删除
            String chromaBase = "http://localhost:8000";
            String tenant = "SpringAiTenant";
            String database = "SpringAiDatabase";
            String collection = "my_collection_v2";

            RestTemplate restTemplate = new RestTemplate();

            // 1. 先拿 collection id
            String listUrl = chromaBase + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
            List collections = restTemplate.getForObject(listUrl, List.class);
            String collectionId = null;
            for (Object obj : collections) {
                Map m = (Map) obj;
                if (collection.equals(m.get("name"))) {
                    collectionId = (String) m.get("id");
                    break;
                }
            }

            if (collectionId == null) {
                return Result.error("collection not found");
            }

            // 2. 按 where 条件删除（POST 方法）
            String deleteUrl = chromaBase + "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections/" + collectionId + "/delete";
            Map<String, Object> body = new HashMap<>();
            body.put("where", Map.of("docId", docId));
            restTemplate.postForObject(deleteUrl, body, String.class);

            Map<String, Object> data = new HashMap<>();
            data.put("docId", docId);
            data.put("status", "deleted");
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
