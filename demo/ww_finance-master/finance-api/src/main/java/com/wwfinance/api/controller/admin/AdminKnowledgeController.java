package com.wwfinance.api.controller.admin;

import com.wwfinance.api.service.KnowledgeBaseService;
import com.wwfinance.api.service.KnowledgeDocService;
import com.wwfinance.common.result.PccAjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台-RAG知识库管理
 * 与前端 ww_finance_admin 对齐：
 *  - GET    /admin/core/knowledge/base/page   知识库列表
 *  - POST   /admin/core/knowledge/base       新增
 *  - PUT    /admin/core/knowledge/base       修改
 *  - DELETE /admin/core/knowledge/base/{id}  删除
 * 文档管理接口（/admin/core/knowledge/doc/**）随详情页后续补充
 */
@Api(tags = "管理后台-RAG知识库管理")
@RestController
@RequestMapping("/admin/core/knowledge")
@Slf4j
public class AdminKnowledgeController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeDocService knowledgeDocService;

    @ApiOperation("知识库列表")
    @GetMapping("/base/page")
    public PccAjaxResult page() {
        return new PccAjaxResult(200, "获取成功", knowledgeBaseService.listAll());
    }

    @ApiOperation("新增知识库")
    @PostMapping("/base")
    public PccAjaxResult add(@RequestBody Map<String, Object> body) {
        knowledgeBaseService.add(str(body.get("name")), str(body.get("description")), str(body.get("embeddingModel")));
        return new PccAjaxResult(200, "新增成功");
    }

    @ApiOperation("修改知识库")
    @PutMapping("/base")
    public PccAjaxResult update(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        knowledgeBaseService.update(id, str(body.get("name")), str(body.get("description")), str(body.get("embeddingModel")));
        return new PccAjaxResult(200, "修改成功");
    }

    @ApiOperation("删除知识库")
    @DeleteMapping("/base/{id}")
    public PccAjaxResult delete(@ApiParam(value = "知识库id", required = true) @PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return new PccAjaxResult(200, "删除成功");
    }

    // ===== 文档管理 =====

    @ApiOperation("某知识库下的文档列表")
    @GetMapping("/doc/page")
    public PccAjaxResult docList(@ApiParam(value = "知识库id", required = true) @RequestParam Long kbId) {
        return new PccAjaxResult(200, "获取成功", knowledgeDocService.listByKbId(kbId));
    }

    @ApiOperation("添加文本文档")
    @PostMapping("/doc/text")
    public PccAjaxResult addText(@RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : Long.valueOf(String.valueOf(body.get("kbId")));
        knowledgeDocService.addText(kbId, str(body.get("title")), str(body.get("content")));
        return new PccAjaxResult(200, "添加成功");
    }

    @ApiOperation("添加PDF文档（元数据，文件解析后续）")
    @PostMapping("/doc/pdf")
    public PccAjaxResult addPdf(@RequestBody Map<String, Object> body) {
        Long kbId = body.get("kbId") == null ? null : Long.valueOf(String.valueOf(body.get("kbId")));
        Long fileSize = body.get("fileSize") == null ? 0L : Long.valueOf(String.valueOf(body.get("fileSize")));
        Long docId = knowledgeDocService.addPdf(kbId, str(body.get("title")), str(body.get("fileName")), fileSize);
        return new PccAjaxResult(200, "上传成功", docId);
    }

    @ApiOperation("删除文档")
    @DeleteMapping("/doc/{id}")
    public PccAjaxResult deleteDoc(@ApiParam(value = "文档id", required = true) @PathVariable Long id) {
        knowledgeDocService.delete(id);
        return new PccAjaxResult(200, "删除成功");
    }

    @ApiOperation("AI 服务回调更新文档处理状态")
    @PostMapping("/doc/updateStatus")
    public PccAjaxResult updateStatus(@RequestBody Map<String, Object> body) {
        Long docId = body.get("docId") == null ? null : Long.valueOf(String.valueOf(body.get("docId")));
        Integer status = body.get("status") == null ? null : Integer.valueOf(String.valueOf(body.get("status")));
        Integer chunkCount = body.get("chunkCount") == null ? null : Integer.valueOf(String.valueOf(body.get("chunkCount")));
        knowledgeDocService.updateStatus(docId, status, chunkCount);
        return new PccAjaxResult(200, "更新成功");
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
