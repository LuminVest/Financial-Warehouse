package com.wwfinance.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wwfinance.api.entity.*;
import com.wwfinance.api.mapper.*;
import com.wwfinance.common.result.PccAjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能客服管理接口
 */
@RestController
@RequestMapping("/admin/core/chat")
public class AdminChatController {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatPromptConfigMapper chatPromptConfigMapper;

    @Autowired
    private ChatModelConfigMapper chatModelConfigMapper;

    // ========== 咨询记录 ==========

    /**
     * 分页查询会话列表
     */
    @GetMapping("/session/list")
    public PccAjaxResult sessionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ChatSession::getUserName, keyword)
                   .or().like(ChatSession::getUserPhone, keyword)
                   .or().like(ChatSession::getTitle, keyword);
        }
        if (status != null) {
            wrapper.eq(ChatSession::getStatus, status);
        }
        wrapper.orderByDesc(ChatSession::getUpdateTime);

        Page<ChatSession> pageResult = chatSessionMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("list", pageResult.getRecords());
        return new PccAjaxResult(200, "获取成功", result);
    }

    /**
     * 获取会话详情（含消息列表）
     */
    @GetMapping("/session/{id}")
    public PccAjaxResult sessionDetail(@PathVariable Long id) {
        ChatSession session = chatSessionMapper.selectById(id);
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, id)
                                                     .orderByAsc(ChatMessage::getCreateTime)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("messages", messages);
        return new PccAjaxResult(200, "获取成功", result);
    }

    /**
     * 结束会话
     */
    @PutMapping("/session/{id}/close")
    public PccAjaxResult closeSession(@PathVariable Long id) {
        ChatSession session = chatSessionMapper.selectById(id);
        session.setStatus(1);
        chatSessionMapper.updateById(session);
        return new PccAjaxResult(200, "操作成功");
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{id}")
    public PccAjaxResult deleteSession(@PathVariable Long id) {
        chatSessionMapper.deleteById(id);
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, id)
        );
        return new PccAjaxResult(200, "删除成功");
    }

    // ========== Prompt 配置 ==========

    /**
     * Prompt 配置列表
     */
    @GetMapping("/prompt/list")
    public PccAjaxResult promptList() {
        List<ChatPromptConfig> list = chatPromptConfigMapper.selectList(
                new LambdaQueryWrapper<ChatPromptConfig>().eq(ChatPromptConfig::getStatus, 1)
                                                         .orderByDesc(ChatPromptConfig::getIsDefault)
        );
        return new PccAjaxResult(200, "获取成功", list);
    }

    /**
     * 新增 Prompt 配置
     */
    @PostMapping("/prompt")
    public PccAjaxResult addPrompt(@RequestBody ChatPromptConfig config) {
        chatPromptConfigMapper.insert(config);
        return new PccAjaxResult(200, "新增成功");
    }

    /**
     * 更新 Prompt 配置
     */
    @PutMapping("/prompt/{id}")
    public PccAjaxResult updatePrompt(@PathVariable Long id, @RequestBody ChatPromptConfig config) {
        config.setId(id);
        chatPromptConfigMapper.updateById(config);
        return new PccAjaxResult(200, "修改成功");
    }

    /**
     * 删除 Prompt 配置
     */
    @DeleteMapping("/prompt/{id}")
    public PccAjaxResult deletePrompt(@PathVariable Long id) {
        chatPromptConfigMapper.deleteById(id);
        return new PccAjaxResult(200, "删除成功");
    }

    /**
     * 设置默认 Prompt
     */
    @PutMapping("/prompt/{id}/default")
    public PccAjaxResult setDefaultPrompt(@PathVariable Long id) {
        // 先把所有设为非默认
        List<ChatPromptConfig> all = chatPromptConfigMapper.selectList(null);
        for (ChatPromptConfig c : all) {
            c.setIsDefault(0);
            chatPromptConfigMapper.updateById(c);
        }
        // 再把当前设为默认
        ChatPromptConfig config = chatPromptConfigMapper.selectById(id);
        config.setIsDefault(1);
        chatPromptConfigMapper.updateById(config);
        return new PccAjaxResult(200, "设置成功");
    }

    // ========== 模型配置 ==========

    /**
     * 模型配置列表
     */
    @GetMapping("/model/list")
    public PccAjaxResult modelList() {
        List<ChatModelConfig> list = chatModelConfigMapper.selectList(
                new LambdaQueryWrapper<ChatModelConfig>().eq(ChatModelConfig::getStatus, 1)
                                                         .orderByDesc(ChatModelConfig::getIsDefault)
        );
        return new PccAjaxResult(200, "获取成功", list);
    }

    /**
     * 新增模型配置
     */
    @PostMapping("/model")
    public PccAjaxResult addModel(@RequestBody ChatModelConfig config) {
        chatModelConfigMapper.insert(config);
        return new PccAjaxResult(200, "新增成功");
    }

    /**
     * 更新模型配置
     */
    @PutMapping("/model/{id}")
    public PccAjaxResult updateModel(@PathVariable Long id, @RequestBody ChatModelConfig config) {
        config.setId(id);
        chatModelConfigMapper.updateById(config);
        return new PccAjaxResult(200, "修改成功");
    }

    /**
     * 删除模型配置
     */
    @DeleteMapping("/model/{id}")
    public PccAjaxResult deleteModel(@PathVariable Long id) {
        chatModelConfigMapper.deleteById(id);
        return new PccAjaxResult(200, "删除成功");
    }

    /**
     * 设置默认模型
     */
    @PutMapping("/model/{id}/default")
    public PccAjaxResult setDefaultModel(@PathVariable Long id) {
        chatModelConfigMapper.clearDefault();
        int rows = chatModelConfigMapper.setDefault(id);
        if (rows == 0) {
            return new PccAjaxResult(500, "模型不存在或已删除");
        }
        return new PccAjaxResult(200, "设置成功");
    }
}
