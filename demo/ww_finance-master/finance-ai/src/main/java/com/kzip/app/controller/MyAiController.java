package com.kzip.app.controller;


import com.kzip.app.repository.ChatHistoryRepository;
import com.kzip.app.repository.LocalPdfFileRepository;
import com.kzip.app.tool.DateTimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


@RestController
@RequestMapping("/ai")
public class MyAiController {

    private final ChatClient chatClient;
    private final  ChatClient pdfChatClient;
    private final LocalPdfFileRepository fileRepository;
    private final ChatHistoryRepository chatRepository;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;



    public MyAiController(ChatClient.Builder chatClientBuilder, ChatClient pdfChatClient, LocalPdfFileRepository fileRepository, ChatHistoryRepository chatRepository, VectorStore vectorStore, EmbeddingModel embeddingModel) {
        // 工具调用需要同步接口才能完整执行"模型请求→调用工具→回传结果→模型生成"这一闭环
        // 流式(.stream) + 工具调用在部分模型上会被截断，因此同步接口更适合验证工具是否生效
        this.chatClient = chatClientBuilder
                .defaultTools(new DateTimeTools())
                .build();
        this.pdfChatClient = pdfChatClient;
        this.fileRepository = fileRepository;
        this.chatRepository = chatRepository;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 同步接口：用于验证工具调用是否正常工作
     * 访问：/ai-sync?userInput=获取当前时间
     */
    @GetMapping("/ai-sync")
    Result<String> generationSync(@RequestParam String userInput) {
        String content = this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
        return Result.success(content);
    }

    /**
     * 流式接口：如果工具调用正常后再考虑切换
     */
    @GetMapping(value = "/ai-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> generation(@RequestParam String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }


    @RequestMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(String prompt, String chatId) {
        chatRepository.save("pdf", chatId);
        Resource file = fileRepository.getFile(chatId);
        return pdfChatClient
                .prompt(prompt)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "file_name == '"+file.getFilename()+"'"))
                .stream()
                .content();
    }

    /**
     * 知识库 RAG 问答：向量检索 topK 最相关片段，拼到 prompt 里
     * GET /ai/knowledge-chat?prompt=xxx&chatId=xxx
     */
    @RequestMapping(value = "/knowledge-chat", produces = "text/html;charset=UTF-8")
    public Flux<String> knowledgeChat(String prompt, String chatId) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            // 1. 用 EmbeddingModel 把用户问题转成向量
            float[] embedding = embeddingModel.embed(prompt);
            java.util.List<java.util.List<Double>> queryEmbeddings = java.util.Collections.singletonList(
                    java.util.stream.IntStream.range(0, embedding.length)
                            .mapToObj(i -> (double) embedding[i])
                            .collect(java.util.stream.Collectors.toList())
            );

            // 2. 调 Chroma query 接口，拿 topK=3 个最相关文档
            String collectionId = "08fb2271-bf77-4a84-9d84-8dc7ab030d1c";
            String queryUrl = "http://localhost:8000/api/v2/tenants/SpringAiTenant/databases/SpringAiDatabase/collections/" + collectionId + "/query";

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("query_embeddings", queryEmbeddings);
            body.put("n_results", 5);
            body.put("include", java.util.List.of("documents", "distances"));

            java.util.Map result = restTemplate.postForObject(queryUrl, body, java.util.Map.class);
            java.util.List documents = (java.util.List) result.get("documents");

            StringBuilder sb = new StringBuilder();
            if (documents != null && !documents.isEmpty()) {
                java.util.List docList = (java.util.List) documents.get(0);
                for (int i = 0; i < docList.size(); i++) {
                    if (i > 0) sb.append("\n\n---\n\n");
                    sb.append(docList.get(i).toString());
                }
            }
            String context = sb.toString();

            System.out.println("【RAG 检索到的片段数】：" + (documents == null || documents.isEmpty() ? 0 : ((java.util.List) documents.get(0)).size()));
            System.out.println("【RAG 检索到的内容】：\n" + context);

            if (context.isEmpty()) {
                return Flux.just("抱歉，这个问题我暂时回答不了。");
            }

            String systemPrompt = "你是旺旺金融的智能客服。请严格根据以下提供的知识库内容回答用户问题。\n"
                    + "如果知识库中没有相关信息，直接说\"抱歉，这个问题我暂时回答不了\"，绝对不要自己编造答案。\n\n"
                    + "===== 知识库内容开始 =====\n" + context + "===== 知识库内容结束 =====";

            var request = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(prompt);

            // 只有传了 chatId 才加对话记忆参数，否则不加
            if (chatId != null && !chatId.isEmpty()) {
                request = request.advisors(a -> a.param(CONVERSATION_ID, chatId));
            }

            return request
                    .stream()
                    .content();
        } catch (Exception e) {
            e.printStackTrace();
            return Flux.just("系统异常: " + e.getMessage());
        }
    }
}
