package com.kzip.app.controller;


import com.kzip.app.repository.ChatHistoryRepository;
import com.kzip.app.repository.LocalPdfFileRepository;
import com.kzip.app.tool.DateTimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


@RestController
@RequestMapping("/ai")
public class MyAiController {

    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final LocalPdfFileRepository fileRepository;
    private final ChatHistoryRepository chatRepository;
    private final VectorStore vectorStore;



    public MyAiController(@org.springframework.beans.factory.annotation.Qualifier("chatClient") ChatClient chatClient,
                          @org.springframework.beans.factory.annotation.Qualifier("deepSeekChatClient") ChatClient ragChatClient,
                          LocalPdfFileRepository fileRepository,
                          ChatHistoryRepository chatRepository,
                          VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
        this.fileRepository = fileRepository;
        this.chatRepository = chatRepository;
        this.vectorStore = vectorStore;
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
        return ragChatClient
                .prompt(prompt)
                .advisors(a -> a.param(CONVERSATION_ID, chatId))
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "file_name == '"+file.getFilename()+"'"))
                .stream()
                .content();
    }

    /**
     * 知识库 RAG 问答（标准写法，自动检索 + 对话记忆）
     * GET /ai/knowledge-chat?prompt=xxx&chatId=xxx
     * 模型由管理端 chat_model_config 表的默认配置决定
     */
    @RequestMapping(value = "/knowledge-chat", produces = "text/html;charset=UTF-8")
    public Flux<String> knowledgeChat(String prompt, String chatId) {
        try {
            String systemPrompt = "你是旺旺金融的智能客服小旺，服务态度友好热情，回答简洁专业。\n"
                    + "你的工作原则：\n"
                    + "1. 业务相关问题请严格根据知识库内容回答，不要编造业务信息。\n"
                    + "2. 知识库没有的业务问题，礼貌告知并引导联系人工客服。\n"
                    + "3. 问候、闲聊等简单对话正常友好回答。\n"
                    + "4. 对话中'我/用户'指客户，'你'指小旺自己，根据上下文准确理解。\n"
                    + "5. 记住之前的对话内容，保持上下文连贯。";

            // 从管理端获取当前默认模型
            String defaultModel = getDefaultModel();
            ChatClient selectedClient = defaultModel != null && defaultModel.toLowerCase().contains("deepseek")
                    ? ragChatClient : chatClient;

            var request = selectedClient
                    .prompt()
                    .system(systemPrompt)
                    .user(prompt);

            // 传 chatId 启用对话记忆
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

    // 缓存默认模型，30秒刷新一次
    private volatile String cachedModel = null;
    private volatile long cacheTime = 0;
    private static final long CACHE_TTL = 30_000; // 30秒

    private String getDefaultModel() {
        long now = System.currentTimeMillis();
        if (cachedModel != null && now - cacheTime < CACHE_TTL) {
            return cachedModel;
        }
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<?, ?> resp = restTemplate.getForObject(
                    "http://localhost:8990/api/core/chat/default-model", java.util.Map.class);
            if (resp != null && resp.get("data") != null) {
                cachedModel = resp.get("data").toString();
                cacheTime = now;
            }
        } catch (Exception e) {
            // 获取失败用上次缓存，没有就默认 qwen
            if (cachedModel == null) cachedModel = "qwen-plus";
        }
        return cachedModel;
    }
}
