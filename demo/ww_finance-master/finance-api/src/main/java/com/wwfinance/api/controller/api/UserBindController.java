package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.UserBindDTO;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.api.utils.RequestHelper;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户绑定（实名 + 银行户，对接旺旺银行托管平台）
 * 接口结构
 *  - GET  /getBindInfo   获取绑定信息
 *  - POST /auth/bind     账户绑定提交数据（返回托管平台表单）
 *  - POST /notify        账户绑定异步回调（第三方回调，免登录）
 */
@Api(tags = "用户绑定")
@RestController
@RequestMapping("/api/core/userBind")
@Slf4j
public class UserBindController {

    @Resource
    private UserBindService userBindService;

    /**
     * 获取绑定信息
     */
    @ApiOperation("获取绑定信息")
    @GetMapping("/getBindInfo")
    public PccAjaxResult getBindInfo() {
        Long userId = LoginUserContext.getUserid().longValue();
        UserBind bindInfo = userBindService.getBindInfoByUserId(userId);
        return new PccAjaxResult(200, "获取成功", bindInfo);
    }

    /**
     * 账户绑定提交数据
     */
    @ApiOperation("账户绑定提交数据")
    @PostMapping("/auth/bind")
    public PccAjaxResult bind(@RequestBody UserBindDTO userBindDTO) {
        Long userId = LoginUserContext.getUserid().longValue();
        String formStr = userBindService.commitBindUser(userBindDTO, userId);
        return new PccAjaxResult(200, "账户提交绑定数据成功", formStr);
    }

    /**
     * 账户绑定异步回调
     * 注意：第三方托管平台回调不应要求登录 token，该路径已在 MyWebConfig 白名单中放行。
     * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试；
     * 若返回 JSON 字符串"success"（带引号），平台会认为失败并重试 5 次。
     */
    @ApiOperation("账户绑定异步回调")
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户账户绑定异步回调：" + JSON.toJSONString(paramMap));
        // 校验签名
        if (!RequestHelper.isSignEquals(paramMap)) {
            log.error("用户账户绑定异步回调签名错误：" + JSON.toJSONString(paramMap));
            return "fail";
        }
        // 修改绑定状态
        userBindService.notify(paramMap);
        return "success";
    }
}
