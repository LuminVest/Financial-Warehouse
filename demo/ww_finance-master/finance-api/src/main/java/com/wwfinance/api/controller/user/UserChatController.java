package com.wwfinance.api.controller.user;

import com.wwfinance.api.entity.ChatMessage;
import com.wwfinance.api.entity.ChatModelConfig;
import com.wwfinance.api.entity.ChatSession;
import com.wwfinance.api.mapper.ChatMessageMapper;
import com.wwfinance.api.mapper.ChatModelConfigMapper;
import com.wwfinance.api.mapper.ChatSessionMapper;
import com.wwfinance.common.result.PccAjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端智能客服聊天记录接口
 */
@RestController
@RequestMapping("/api/core/chat")
public class UserChatController {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatModelConfigMapper chatModelConfigMapper;

    /**
     * 获取当前默认模型（finance-ai 调用，决定用哪个 ChatClient）
     */
    @GetMapping("/default-model")
    public PccAjaxResult getDefaultModel() {
        ChatModelConfig config = chatModelConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatModelConfig>()
                        .eq(ChatModelConfig::getIsDefault, 1)
                        .eq(ChatModelConfig::getStatus, 1)
                        .last("LIMIT 1")
        );
        String modelName = config != null ? config.getModelName() : "qwen-plus";
        return new PccAjaxResult(200, "获取成功", modelName);
    }

    /**
     * 保存聊天记录（用户端对话完成后调用）
     */
    @PostMapping("/save")
    public PccAjaxResult saveChatRecord(@RequestBody ChatSaveRequest request) {
        ChatSession session;
        
        if (request.getSessionId() != null && request.getSessionId() > 0) {
            // 更新已有会话
            session = chatSessionMapper.selectById(request.getSessionId());
            if (session == null) {
                session = new ChatSession();
            }
        } else {
            // 新建会话
            session = new ChatSession();
        }

        // 只有当请求里传了对应字段的时候才更新，避免覆盖成 null
        if (request.getUserId() != null) {
            session.setUserId(request.getUserId());
        }
        if (request.getUserName() != null) {
            session.setUserName(request.getUserName());
        }
        if (request.getUserPhone() != null) {
            session.setUserPhone(request.getUserPhone());
        }
        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        if (request.getKbName() != null) {
            session.setKbName(request.getKbName());
        }
        if (request.getModel() != null) {
            session.setModel(request.getModel());
        }
        session.setStatus(0); // 进行中

        if (session.getId() == null) {
            chatSessionMapper.insert(session);
        } else {
            chatSessionMapper.updateById(session);
        }

        // 保存消息
        if (request.getMessage() != null) {
            ChatMessage message = new ChatMessage();
            message.setSessionId(session.getId());
            message.setRole(request.getMessage().getRole());
            message.setContent(request.getMessage().getContent());
            chatMessageMapper.insert(message);

            // 更新会话消息数
            int count = session.getMessageCount() == null ? 0 : session.getMessageCount();
            session.setMessageCount(count + 1);
            chatSessionMapper.updateById(session);
        }

        return new PccAjaxResult(200, "保存成功", session.getId());
    }

    public static class ChatSaveRequest {
        private Long sessionId;
        private Long userId;
        private String userName;
        private String userPhone;
        private String title;
        private String kbName;
        private String model;
        private MessageDTO message;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserPhone() { return userPhone; }
        public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getKbName() { return kbName; }
        public void setKbName(String kbName) { this.kbName = kbName; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public MessageDTO getMessage() { return message; }
        public void setMessage(MessageDTO message) { this.message = message; }
    }

    public static class MessageDTO {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
