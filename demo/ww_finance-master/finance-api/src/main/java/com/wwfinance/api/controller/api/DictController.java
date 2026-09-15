package com.wwfinance.api.controller.api;

import com.wwfinance.api.service.DictService;
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

/**
 * 前台-数据字典（下拉枚举）
 *  - GET /api/core/dict/listByDictCode/{dictCode}  按分类编码查字典项
 */
@Api(tags = "前台-数据字典")
@RestController
@RequestMapping("/api/core/dict")
@Slf4j
public class DictController {

    @Autowired
    private DictService dictService;

    @ApiOperation("按分类编码查询字典项列表")
    @GetMapping("/listByDictCode/{dictCode}")
    public PccAjaxResult listByDictCode(
            @ApiParam(value = "分类编码(如 industry/education/income)", required = true)
            @PathVariable String dictCode) {
        return new PccAjaxResult(200, "获取成功", dictService.listByDictCode(dictCode));
    }
}
