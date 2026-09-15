package com.wwfinance.api.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.wwfinance.api.entity.dto.ExcelDictDTO;
import com.wwfinance.api.service.DictService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理后台-数据字典管理
 * 与前端 ww_admin 对齐：
 *  - GET  /admin/core/dict/tree          树形结构
 *  - POST /admin/core/dict               新增
 *  - PUT  /admin/core/dict               修改
 *  - DELETE /admin/core/dict/{id}        删除
 *  - GET  /admin/core/dict/export        导出 Excel
 *  - POST /admin/core/dict/import        导入 Excel
 */
@Api(tags = "管理后台-数据字典管理")
@RestController
@RequestMapping("/admin/core/dict")
@Slf4j
public class AdminDictController {

    @Autowired
    private DictService dictService;

    @ApiOperation("字典树形结构")
    @GetMapping("/tree")
    public PccAjaxResult tree() {
        return new PccAjaxResult(200, "获取成功", dictService.listTree());
    }

    @ApiOperation("新增字典")
    @PostMapping
    public PccAjaxResult add(@RequestBody Map<String, Object> body) {
        dictService.add(longV(body.get("parentId")), str(body.get("name")),
                intV(body.get("value")), str(body.get("dictCode")));
        return new PccAjaxResult(200, "新增成功");
    }

    @ApiOperation("修改字典")
    @PutMapping
    public PccAjaxResult update(@RequestBody Map<String, Object> body) {
        dictService.update(longV(body.get("id")), longV(body.get("parentId")),
                str(body.get("name")), intV(body.get("value")), str(body.get("dictCode")));
        return new PccAjaxResult(200, "修改成功");
    }

    @ApiOperation("删除字典")
    @DeleteMapping("/{id}")
    public PccAjaxResult delete(@ApiParam(value = "字典id", required = true) @PathVariable Long id) {
        dictService.delete(id);
        return new PccAjaxResult(200, "删除成功");
    }

    @ApiOperation("导出字典 Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        // 将树形结构展平成行：分类行 + 字典项行
        List<ExcelDictDTO> rows = flatten(dictService.listTree());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("数据字典", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ExcelDictDTO.class)
                .sheet("数据字典")
                .doWrite(rows);
    }

    @ApiOperation("导入字典 Excel")
    @PostMapping("/import")
    public PccAjaxResult importExcel(
            @ApiParam(value = "Excel 文件", required = true) @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new PccAjaxResult(500, "请选择要导入的 Excel 文件");
        }
        try (InputStream in = file.getInputStream()) {
            List<ExcelDictDTO> list = new ArrayList<>();
            EasyExcel.read(in, ExcelDictDTO.class, new com.alibaba.excel.event.AnalysisEventListener<ExcelDictDTO>() {
                @Override
                public void invoke(ExcelDictDTO data, com.alibaba.excel.context.AnalysisContext context) {
                    list.add(data);
                }

                @Override
                public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
                    log.info("字典 Excel 解析完成，共 {} 行", list.size());
                }
            }).sheet().doRead();
            int count = dictService.importExcel(list);
            return new PccAjaxResult(200, "导入成功，共处理 " + count + " 条");
        } catch (IOException e) {
            log.error("字典 Excel 导入失败", e);
            return new PccAjaxResult(500, "导入失败: " + e.getMessage());
        }
    }

    private List<ExcelDictDTO> flatten(List<com.wwfinance.api.entity.dto.DictDTO> roots) {
        List<ExcelDictDTO> rows = new ArrayList<>();
        for (com.wwfinance.api.entity.dto.DictDTO node : roots) {
            collect(node, null, rows);
        }
        return rows;
    }

    private void collect(com.wwfinance.api.entity.dto.DictDTO node, String parentCode, List<ExcelDictDTO> rows) {
        boolean isCategory = node.getDictCode() != null && !node.getDictCode().isEmpty();
        ExcelDictDTO row = new ExcelDictDTO();
        row.setName(node.getName());
        row.setValue(node.getValue());
        if (isCategory) {
            row.setDictCode(node.getDictCode());
        } else {
            row.setParentDictCode(parentCode);
        }
        rows.add(row);
        // 子节点递归（分类节点的子项携带父编码）
        for (com.wwfinance.api.entity.dto.DictDTO child : node.getChildren()) {
            collect(child, isCategory ? node.getDictCode() : parentCode, rows);
        }
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Integer intV(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    private Long longV(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
