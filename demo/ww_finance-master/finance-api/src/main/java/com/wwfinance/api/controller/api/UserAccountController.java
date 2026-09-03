package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.service.UserAccountService;
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
import java.math.BigDecimal;
import java.util.Map;


@Slf4j
@Api(tags = "用户账户")
@RestController
@RequestMapping("/api/user/account")
public class UserAccountController {

    @Resource
    private UserAccountService userAccountService;

    @Autowired
    private UserService userService;

    private static TokenUtil tu = new TokenUtil();

    /**
     * 充值（生成托管平台表单）
     */
    @ApiOperation("充值")
    @RequestMapping(value = "/auth/commitCharge", method = {RequestMethod.GET, RequestMethod.POST})
    public PccAjaxResult commitCharge(
            @RequestParam BigDecimal chargeAmt,
            @RequestHeader("Authorization") String authorizationHeader) {
        // 获取 Authorization 头部
        String token = authorizationHeader;
        log.info("token:" + token);
        // 通过 token 获取手机号
        Map phone = tu.getMapInfoFromToken(token);
        log.info(phone.toString());
        String mobile = (String) phone.get("token_phone");
        // 反查当前登录用户（兼容 demo 的 token_userid）
        User user = getUserByPhoneOrId(mobile, phone);
        // 调服务层生成充值托管平台表单
        String formStr = userAccountService.commitCharge(chargeAmt, user.getId());
        return new PccAjaxResult(200, "账户提交充值数据成功", formStr);
    }

    /**
     * 充值异步回调
     * 注意：第三方支付回调一般不应要求登录 token，
     * 接入真实支付后，需要把本路径加入 JwtAuthInterceptor 白名单。
     * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试。
     */
    @ApiOperation("充值异步回调")
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(HttpServletRequest request) {
        // 请求参数封装到 map 集合中
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户充值异步回调：" + JSON.toJSONString(paramMap));
        // 验签（demo 简化：接入支付平台后在 RequestHelper 中实现真实验签）
        if (RequestHelper.isSignEquals(paramMap)) {
            // 判断业务是否成功
            if ("0001".equals(paramMap.get("resultCode"))) {
                // 同步账户数据
                return userAccountService.notify(paramMap);
            } else {
                return "success";
            }
        } else {
            return "fail";
        }
    }

    /**
     * 查询账户余额
     */
    @ApiOperation("查询账户余额")
    @GetMapping("/auth/getAccount")
    public PccAjaxResult getAccount(@RequestHeader("Authorization") String authorizationHeader) {
        // 获取 Authorization 头部
        String token = authorizationHeader;
        log.info("token:" + token);
        // 通过 token 获取手机号
        Map phone = tu.getMapInfoFromToken(token);
        log.info(phone.toString());
        String mobile = (String) phone.get("token_phone");
        // 反查当前登录用户
        User user = getUserByPhoneOrId(mobile, phone);
        // 查账户余额
        BigDecimal account = userAccountService.getAccount(user.getId());
        return new PccAjaxResult(200, "查询账户余额", account);
    }

    /**
     * 兼容两种 token：
     *  - 老师版：claims 存 token_phone(手机号)，按手机号反查
     *  - demo 版：claims 存 token_userid(用户ID)，直接按 ID 查
     */
    private User getUserByPhoneOrId(String mobile, Map phone) {
        if (mobile != null && !mobile.isEmpty()) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getMobile, mobile);
            User user = userService.getOne(queryWrapper);
            if (user != null) {
                return user;
            }
        }
        Object uid = phone.get("token_userid");
        if (uid == null) {
            throw new RuntimeException("token 中未找到用户信息");
        }
        return userService.getById(Long.valueOf(String.valueOf(uid)));
    }
}
