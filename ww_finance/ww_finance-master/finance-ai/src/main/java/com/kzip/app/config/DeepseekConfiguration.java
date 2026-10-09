package com.kzip.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek 模型专用 ChatClient：带向量检索 + 对话记忆
 * 由 spring-ai-starter-model-deepseek 自动装配出 deepSeekChatModel，
 * 这里通过 @Qualifier 明确注入，与 DashScope 的 dashScopeChatModel 并存。
 */
@Configuration
public class DeepseekConfiguration {

    @Bean
    public ChatClient deepSeekChatClient(
            @Qualifier("deepSeekChatModel") ChatModel model,
            ChatMemory chatMemory,
            VectorStore vectorStore) {

        return ChatClient.builder(model)
                .defaultAdvisors(
                        // RAG 向量检索
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.0d)
                                        .topK(5)
                                        .build())
                                .build(),
                        // 对话记忆
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}
