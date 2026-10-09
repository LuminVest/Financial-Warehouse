package com.kzip.app.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 持久化对话记忆
 * 重启服务对话也不会丢。
 *
 * 存储格式：每个会话是一个 Redis List，每个元素是一段 JSON 字符串：
 *   {"type":"USER","text":"你好"}
 * 之所以不直接把 Spring AI 的 Message 对象塞进 RedisTemplate，是因为：
 *   - UserMessage 等类没有实现 java.io.Serializable，JDK 序列化器写不进去；
 *   - 这些类也没有无参构造函数，Jackson 反序列化器实例化不了。
 * 因此在仓储层做一次 Message <-> DTO(JSON) 的手工转换。
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long EXPIRE_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Message> result = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o == null) {
                continue;
            }
            try {
                result.add(parseDto(o.toString()));
            } catch (Exception e) {
                // 单条脏数据跳过，不影响整个会话
            }
        }
        return result;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        // 每次都全量覆盖：MessageWindowChatMemory 维护的是滑动窗口
        redisTemplate.delete(key);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        // 注意：必须用 List<Object>，不能用 List<String>。
        // RedisTemplate<String,Object> 的 rightPushAll(K, Collection<Object>) 因泛型不变性
        // 不接受 List<String>，会被错配到 rightPushAll(K, Object...) 可变参重载，
        // 把整个 List 当成单个元素序列化，触发 ClassCastException。
        List<Object> entries = new ArrayList<>(messages.size());
        for (Message m : messages) {
            try {
                Map<String, Object> dto = new HashMap<>();
                dto.put("type", m.getMessageType().name());
                dto.put("text", m.getText());
                entries.add(objectMapper.writeValueAsString(dto));
            } catch (Exception ignored) {
                // 单条转换失败跳过
            }
        }
        if (!entries.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, entries);
        }
        redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private Message parseDto(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        String type = node.path("type").asText("USER");
        String text = node.path("text").asText("");
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            case SYSTEM -> new SystemMessage(text);
            // TOOL 消息不参与对话记忆回放，兜底成 AssistantMessage
            case TOOL -> new AssistantMessage(text);
        };
    }
}
