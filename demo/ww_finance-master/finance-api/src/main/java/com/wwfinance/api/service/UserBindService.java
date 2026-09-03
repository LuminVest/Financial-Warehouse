package com.wwfinance.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.UserBindDTO;

import java.util.Map;

public interface UserBindService extends IService<UserBind> {

    /**
     * 根据用户 id 查询绑定记录
     */
    UserBind getBindByUserId(Long userId);

    /**
     * 提交绑定（保存绑定申请 + 构建托管平台表单）
     *
     * @return 托管平台自动提交表单
     */
    String commitBindUser(UserBindDTO userBindDTO, Long userId);

    /**
     * 绑定异步回调：更新绑定状态与协议号
     */
    void notify(Map<String, Object> paramMap);
}
