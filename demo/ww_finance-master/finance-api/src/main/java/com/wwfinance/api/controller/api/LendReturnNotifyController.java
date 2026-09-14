package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.utils.RequestHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 还款异步回调
 * 注意：第三方托管平台回调不应要求登录 token，该路径已在 MyWebConfig 白名单放行。
 * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试。
 */
@Api(tags = "还款回调")
@RestController
@RequestMapping("/api/user/lendReturn")
@Slf4j
public class LendReturnNotifyController {

    @Autowired
    private LendReturnService lendReturnService;

    @ApiOperation("还款异步回调")
    @PostMapping(value = "/notifyUrl", produces = "text/plain")
    public String notifyUrl(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("还款异步回调：" + JSON.toJSONString(paramMap));
        return lendReturnService.notifyRepayment(paramMap);
    }
}
