package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.entity.dto.RepaymentDTO;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.api.utils.LoginUserContext;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "还款计划")
@RestController
@RequestMapping("/api/core/lendReturn")
@Slf4j
public class LendReturnController {

    @Autowired
    private LendReturnService lendReturnService;

    @ApiOperation("按标的查询还款计划列表")
    @GetMapping("/list/{lendId}")
    public PccAjaxResult list(
            @ApiParam(value = "标的id", required = true) @PathVariable Long lendId) {
        List<LendReturn> list = lendReturnService.listByLendId(lendId);
        return new PccAjaxResult(200, "获取还款计划", list);
    }

    @ApiOperation("借款人发起还款（返回托管平台还款表单）")
    @PostMapping("/auth/commitRepayment")
    public PccAjaxResult commitRepayment(
            @ApiParam(value = "还款信息", required = true) @RequestBody RepaymentDTO repaymentDTO) {
        Long userId = LoginUserContext.getUserid().longValue();
        String formStr = lendReturnService.commitRepayment(
                repaymentDTO.getLendId(), repaymentDTO.getCurrentPeriod(), userId);
        return new PccAjaxResult(200, "账户提交还款数据成功", formStr);
    }
}
