package com.wwfinance.api.controller.admin;

import com.wwfinance.api.entity.LendItemReturn;
import com.wwfinance.api.service.LendItemReturnService;
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

@Api(tags = "回款明细管理（后台）")
@RestController
@RequestMapping("/admin/core/lendItemReturn")
@Slf4j
public class AdminLendItemReturnController {

    @Autowired
    private LendItemReturnService lendItemReturnService;

    @ApiOperation("按标的查询回款明细列表")
    @GetMapping("/list/{lendId}")
    public PccAjaxResult list(
            @ApiParam(value = "标的id", required = true) @PathVariable Long lendId) {
        List<LendItemReturn> list = lendItemReturnService.listByLendId(lendId);
        return new PccAjaxResult(200, "获取回款明细", list);
    }
}
