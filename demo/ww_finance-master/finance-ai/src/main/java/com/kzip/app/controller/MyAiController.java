package com.kzip.app.controller;


import com.kzip.app.repository.ChatHistoryRepository;
import com.kzip.app.repository.LocalPdfFileRepository;
import com.kzip.app.tool.DateTimeTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;


@Slf4j
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
    public ResponseEntity<Flux<String>> knowledgeChat(String prompt, String chatId) {
        try {
            String systemPrompt = "你是旺旺金融的智能客服小旺，服务态度友好热情，回答简洁专业。\n"
                    + "你的工作原则：\n"
                    + "1. 业务相关问题请严格根据知识库内容回答，不要编造业务信息。\n"
                    + "2. 知识库没有的业务问题，礼貌告知并引导联系人工客服。\n"
                    + "3. 问候、闲聊等简单对话正常友好回答。\n"
                    + "4. 用户询问'你能做什么/你是谁'时，介绍自己是旺旺金融智能客服小旺，可解答借款申请、投资理财、充值提现、还款计划、账户绑定等业务问题。\n"
                    + "5. 对话中'我/用户'指客户，'你'指小旺自己，根据上下文准确理解。\n"
                    + "6. 记住之前的对话内容，保持上下文连贯。";

            // 从管理端获取当前默认模型
            String defaultModel = getDefaultModel();
            ChatClient selectedClient = defaultModel != null && defaultModel.toLowerCase().contains("deepseek")
                    ? ragChatClient : chatClient;

            // 0. 闲聊/问候/能力询问：直接模型友好回答，不强制走知识库
            if (isSmallTalk(prompt)) {
                log.info("闲聊问题，直接模型回答: chatId={}, prompt={}", chatId, prompt);
                var talk = selectedClient
                        .prompt()
                        .system(systemPrompt)
                        .user(prompt);
                if (chatId != null && !chatId.isEmpty()) {
                    talk = talk.advisors(a -> a.param(CONVERSATION_ID, chatId));
                }
                return okResponse(talk.stream().content());
            }

            // 1. 先检索知识库：命中才走 RAG，未命中直接兜底，不让模型自由发挥
            //    阈值说明：Chroma 返回 L2 distance，Spring AI 相似度 = 1 - distance，
            //    当前语料 top 命中相似度约 0.2~0.3，阈值取 0.20（过低会误命中，过高会漏检）
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(prompt)
                    .topK(5)
                    .similarityThreshold(0.20d)
                    .build();
            List<Document> hits = vectorStore.similaritySearch(searchRequest);
            if (hits == null || hits.isEmpty()) {
                log.info("知识库未命中，引导人工客服: chatId={}, prompt={}", chatId, prompt);
                return okResponse(Flux.just("这个问题有点超出我的服务范围啦，为了给您准确的答复，建议您联系在线人工客服（客服热线：400-8123-888），会有专人帮您处理~ 当然，您也可以换个说法再问我一次，我再帮您看看~"));
            }
            log.info("知识库命中 {} 段，进入RAG回答: chatId={}, prompt={}", hits.size(), chatId, prompt);

            // 命中的文档名（去重、最多3个），随响应头 X-KB-Docs 返回给前端存咨询记录
            String docNames = hits.stream()
                    .map(d -> d.getMetadata() == null ? null : d.getMetadata().get("fileName"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining("、"));
            log.info("命中知识文档: {} (chatId={})", docNames, chatId);

            var request = selectedClient
                    .prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    // 2. 统一挂知识库检索 advisor：无论千问还是 DeepSeek 都基于检索片段回答
                    .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                            .searchRequest(SearchRequest.builder()
                                    .topK(5)
                                    .similarityThreshold(0.20d)
                                    .build())
                            .build());

            // 3. 传 chatId 启用对话记忆
            if (chatId != null && !chatId.isEmpty()) {
                request = request.advisors(a -> a.param(CONVERSATION_ID, chatId));
            }

            return okResponse(request.stream().content(), docNames);
        } catch (Exception e) {
            e.printStackTrace();
            return okResponse(Flux.just("系统异常: " + e.getMessage()));
        }
    }

    /** 统一构造响应：把命中的知识文档名放进 X-KB-Docs 响应头，供前端咨询记录展示
     *  HTTP 响应头只允许 ISO-8859-1 范围，中文文档名必须先 URL 编码，前端再解码 */
    private ResponseEntity<Flux<String>> okResponse(Flux<String> flux, String... docNames) {
        HttpHeaders headers = new HttpHeaders();
        if (docNames != null && docNames.length > 0 && docNames[0] != null && !docNames[0].isEmpty()) {
            headers.add("X-KB-Docs", URLEncoder.encode(docNames[0], StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok().headers(headers).body(flux);
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

    /**
     * 闲聊/问候/能力询问识别：这类问题不强制走知识库，直接由模型友好回答。
     * 命中返回 true；业务问题返回 false，继续走知识库检索。
     */
    private boolean isSmallTalk(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return false;
        }
        String p = prompt.trim().toLowerCase();
        String[] words = {
                "你好", "您好", "在吗", "在不在", "嗨", "哈喽", "hello", "hi",
                "你是谁", "你叫什么", "你能做什么", "你会什么", "你能干嘛", "介绍一下你",
                "谢谢", "感谢", "再见", "拜拜", "辛苦了", "好的", "嗯嗯", "在的"
        };
        for (String w : words) {
            if (p.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
