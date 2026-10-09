package com.wwfinance.controller.api;

import com.wwfinance.client.CoreUserInfoClient;
import com.wwfinance.common.result.PccAjaxResult;
import com.wwfinance.util.SMSUtil;
import com.wwfinance.util.ValidateCodeUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms/user")
@Api(tags = "短信管理")
//@CrossOrigin //跨域
@Slf4j
public class ApiSmsController {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private SMSUtil smsUtil;

    @Resource
    private CoreUserInfoClient coreUserInfoClient;

    @ApiOperation("获取验证码")
    @PostMapping("/sendSMS")
    public PccAjaxResult send(
            @ApiParam(value = "手机号", required = true)
            @RequestParam String mobile){

        if(mobile == null || mobile.trim().isEmpty()) {
            return new PccAjaxResult(500, "手机号不能为空");
        }

        boolean result = coreUserInfoClient.checkMobile(mobile);
        if(!result) {
            return new PccAjaxResult(500, "手机号已被注册");
        }

        // 生成随机的四位验证码
        String code2 = String.valueOf(ValidateCodeUtils.generateValidateCode(4));
        // 调用容联云提供的短信服务API完成发送短信；短信通道不可用（未配置/欠费/失败）时本地兜底
        String code = smsUtil.sendMsg(mobile, code2);
        if (code == null || code.isEmpty()) {
            code = code2;
            log.info("短信通道不可用，本地兜底生成验证码, mobile={}", mobile);
        }
        //将验证码存入redis（key 与 finance-api 注册校验约定一致）
        redisTemplate.opsForValue().set("xx:code:" + mobile, code, 5, TimeUnit.MINUTES);
        // demo 联调：data 返回验证码；生产环境短信下发后应移除 data
        return new PccAjaxResult(200, "手机验证码短信发送成功", code);
    }

    @ApiOperation("发送催还款信息")
    @GetMapping("/sendMsg/{mobile}")
    public String sendMsg(
            @ApiParam(value = "手机号", required = true)
            @PathVariable String mobile){
        //调用容联云提供的短信服务API完成发送短信
        String code = "还款时间剩余天数小于等于3天，请尽快还款";
        String msg = smsUtil.sendMsg(mobile, code);

        return msg;
    }
}
