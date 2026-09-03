package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.UserBindDTO;
import com.wwfinance.api.mapper.UserBindMapper;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.RequestHelper;
import com.wwfinance.api.utils.TokenUtil;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户绑定（实名 + 银行卡，对接旺旺银行托管平台）
 * 接口结构与老师版一致：
 *  - GET  /getBindInfo   获取绑定信息
 *  - POST /auth/bind     账户绑定提交数据（返回托管平台表单）
 *  - POST /notify        账户绑定异步回调（第三方回调，免登录）
 */
@Api(tags = "用户绑定")
@RestController
@RequestMapping("/api/user/bind")
@Slf4j
public class UserBindController {

    @Resource
    private UserBindService userBindService;

    @Resource
    private UserBindMapper userBindMapper;

    @Autowired
    private UserService userService;

    private static TokenUtil tu = new TokenUtil();

    /**
     * 获取绑定信息
     */
    @ApiOperation("获取绑定信息")
    @GetMapping("/getBindInfo")
    public PccAjaxResult getBindInfo(@RequestHeader("Authorization") String authorizationHeader) {
        // 获取 Authorization 头部
        String token = authorizationHeader;
        log.info("token:" + token);
        // 通过 token 获取手机号
        Map phone = tu.getMapInfoFromToken(token);
        log.info(phone.toString());
        String mobile = (String) phone.get("token_phone");
        // 反查当前登录用户（兼容 demo 的 token_userid）
        User user = tu.getUserByPhoneOrId(mobile, phone, userService);
        Long userId = user.getId();
        // 获取绑定信息
        UserBind bindInfoByUserId = userBindMapper.getBindInfoByUserId(userId);
        return new PccAjaxResult(200, "获取成功", bindInfoByUserId);
    }

    /**
     * 账户绑定提交数据
     */
    @ApiOperation("账户绑定提交数据")
    @PostMapping("/auth/bind")
    public PccAjaxResult bind(@RequestBody UserBindDTO userBindDTO,
                              @RequestHeader("Authorization") String authorizationHeader) {
        // 获取 Authorization 头部
        String token = authorizationHeader;
        log.info("token:" + token);
        // 通过 token 获取手机号
        Map phone = tu.getMapInfoFromToken(token);
        log.info(phone.toString());
        String mobile = (String) phone.get("token_phone");
        // 反查当前登录用户（兼容 demo 的 token_userid）
        User user = tu.getUserByPhoneOrId(mobile, phone, userService);
        Long userId = user.getId();
        // 调服务层：保存绑定申请 + 构建托管平台表单
        String formStr = userBindService.commitBindUser(userBindDTO, userId);
        return new PccAjaxResult(200, "账户提交绑定数据成功", formStr);
    }

    /**
     * 账户绑定异步回调
     * 注意：第三方托管平台回调不应要求登录 token，
     * 该路径已在 MyWebConfig 白名单中放行。
     * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试；
     * 若返回 JSON 字符串 "success"（带引号），平台会认为失败并重试 5 次。
     */
    @ApiOperation("账户绑定异步回调")
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(HttpServletRequest request) {
        // 请求参数封装到 map 集合中
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户账号绑定异步回调：" + JSON.toJSONString(paramMap));
        // 校验签名
        if (!RequestHelper.isSignEquals(paramMap)) {
            log.error("用户账号绑定异步回调签名错误：" + JSON.toJSONString(paramMap));
            return "fail";
        }
        // 修改绑定状态
        userBindService.notify(paramMap);
        return "success";
    }
}
