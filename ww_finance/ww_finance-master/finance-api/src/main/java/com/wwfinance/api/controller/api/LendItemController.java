package com.wwfinance.api.controller.api;

import com.alibaba.fastjson.JSON;
import com.wwfinance.api.entity.dto.InvestDTO;
import com.wwfinance.api.service.LendItemService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.api.utils.RequestHelper;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "投资")
@RestController
@RequestMapping("/api/core/lendItem")
@Slf4j
public class LendItemController {

    @Autowired
    private LendItemService lendItemService;

    @ApiOperation("某标的的投资记录列表")
    @GetMapping("/list/{lendId}")
    public PccAjaxResult list(@ApiParam(value = "标的id", required = true) @PathVariable Long lendId) {
        List list = lendItemService.getListByLendId(lendId);
        return new PccAjaxResult(200, "获取投资记录列表", list);
    }

    @ApiOperation("提交投资（返回托管平台表单）")
    @PostMapping("/auth/commitInvest")
    public PccAjaxResult commitInvest(
            @ApiParam(value = "投资信息", required = true) @RequestBody InvestDTO investDTO) {
        Long userId = LoginUserContext.getUserid().longValue();
        String formStr = lendItemService.commitInvest(investDTO, userId);
        return new PccAjaxResult(200, "账户提交投标数据成功", formStr);
    }

    /**
     * 投资异步回调
     * 注意：第三方托管平台回调不应要求登录 token，该路径已在 MyWebConfig 白名单放行。
     * 必须返回纯文本 success（小写，不带引号），托管平台收到后停止重试。
     */
    @ApiOperation("投资异步回调")
    @PostMapping(value = "/notify", produces = "text/plain")
    public String notify(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("用户投标异步回调：" + JSON.toJSONString(paramMap));
        return lendItemService.notify(paramMap);
    }
}
