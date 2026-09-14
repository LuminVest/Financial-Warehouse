package com.wwfinance.api.controller.api;

import com.wwfinance.api.entity.LendReturn;
import com.wwfinance.api.service.LendReturnService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
