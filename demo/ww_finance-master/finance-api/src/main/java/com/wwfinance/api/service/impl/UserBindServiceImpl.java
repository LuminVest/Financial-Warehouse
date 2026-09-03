package com.wwfinance.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.UserBindDTO;
import com.wwfinance.api.enums.UserBindEnum;
import com.wwfinance.api.mapper.UserBindMapper;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.FormHelper;
import com.wwfinance.api.utils.HfbConst;
import com.wwfinance.api.utils.RequestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserBindServiceImpl extends ServiceImpl<UserBindMapper, UserBind> implements UserBindService {

    @Autowired
    private UserService userService;

    @Override
    public UserBind getBindByUserId(Long userId) {
        LambdaQueryWrapper<UserBind> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBind::getUserId, userId);
        return this.getOne(wrapper);
    }

    /**
     * 提交绑定：
     * 1. 保存/同步 user_bind 待绑定记录（status = NO_BIND）
     * 2. 组装旺旺银行托管平台参数（含签名）
     * 3. 构建自动提交表单返回前端（前端跳转托管平台完成开户）
     */
    @Override
    public String commitBindUser(UserBindDTO userBindDTO, Long userId) {
        // 1. 查询是否已有绑定记录
        UserBind userBind = getBindByUserId(userId);
        if (userBind == null) {
            // 首次提交：新建待绑定记录
            userBind = new UserBind();
            BeanUtils.copyProperties(userBindDTO, userBind);
            userBind.setUserId(userId);
            userBind.setStatus(UserBindEnum.NO_BIND.getStatus());
            userBind.setCreateTime(LocalDateTime.now());
            userBind.setUpdateTime(LocalDateTime.now());
            userBind.setDeleted(false);
            baseMapper.insert(userBind);
        } else {
            // 曾经跳转到托管平台，但是未操作完成，此时将用户最新填写的数据同步到userBind对象
            BeanUtils.copyProperties(userBindDTO, userBind);
            userBind.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(userBind);
        }

        // 2. 组装托管平台参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentUserId", userId);
        paramMap.put("idCard", userBindDTO.getIdCard());
        paramMap.put("personalName", userBindDTO.getName());
        paramMap.put("bankType", userBindDTO.getBankType());
        paramMap.put("bankNo", userBindDTO.getBankNo());
        paramMap.put("mobile", userBindDTO.getMobile());
        paramMap.put("returnUrl", HfbConst.USERBIND_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.USERBIND_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        paramMap.put("sign", RequestHelper.getSign(paramMap));

        // 3. 构建自动提交表单（真实项目由前端跳转托管平台，demo 返回表单便于联调）
        String formStr = FormHelper.buildForm(HfbConst.USERBIND_URL, paramMap);
        log.info("构建用户绑定托管表单, userId={}, formStr={}", userId, formStr);
        return formStr;
    }

    /**
     * 绑定异步回调：更新 user_bind 与 user 表的绑定状态、协议号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notify(Map<String, Object> paramMap) {
        log.info("用户绑定异步回调, paramMap={}", JSON.toJSONString(paramMap));
        // 托管平台回调返回：bindCode（绑定协议号）、agentUserId（用户ID）
        String bindCode = String.valueOf(paramMap.get("bindCode"));
        Long userId = Long.valueOf(String.valueOf(paramMap.get("agentUserId")));

        // 1. 更新 user_bind 绑定状态
        UserBind userBind = getBindByUserId(userId);
        if (userBind != null) {
            userBind.setBindCode(bindCode);
            userBind.setStatus(UserBindEnum.BIND_OK.getStatus());
            userBind.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(userBind);
        }
        // 2. 同步 user 表的绑定状态与协议号
        User user = userService.getById(userId);
        if (user != null) {
            user.setBindCode(bindCode);
            user.setBindStatus(UserBindEnum.BIND_OK.getStatus());
            // 同步实名信息（姓名/身份证）到 user 表，便于 getInfo 直接返回
            if (userBind != null) {
                user.setName(userBind.getName());
                user.setIdCard(userBind.getIdCard());
            }
            userService.updateById(user);
        }
    }
}
