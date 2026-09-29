package com.wwfinance.api.result;

import com.wwfinance.common.result.PccAjaxResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一返回结果 PccAjaxResult 单元测试
 * （教学大纲阶段二任务：为统一返回结果补充单元测试）
 *
 * 覆盖：构造方法、success/error 静态方法、链式 put、data 为空时的行为
 */
class PccAjaxResultTest {

    @Test
    void 无参构造应返回空结果() {
        PccAjaxResult result = new PccAjaxResult();
        assertTrue(result.isEmpty(), "无参构造不应包含任何键");
    }

    @Test
    void 双参构造应封装code和msg() {
        PccAjaxResult result = new PccAjaxResult(200, "操作成功");
        assertEquals(200, result.get("code"));
        assertEquals("操作成功", result.get("msg"));
        assertFalse(result.containsKey("data"), "未传 data 时不应有 data 键");
    }

    @Test
    void 三参构造应封装code_msg_data() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);
        PccAjaxResult result = new PccAjaxResult(200, "查询成功", data);
        assertEquals(200, result.get("code"));
        assertEquals("查询成功", result.get("msg"));
        assertSame(data, result.get("data"), "data 应与传入对象一致");
    }

    @Test
    void data为null时不应写入data键() {
        PccAjaxResult result = new PccAjaxResult(500, "系统异常", null);
        assertEquals(500, result.get("code"));
        assertEquals("系统异常", result.get("msg"));
        assertFalse(result.containsKey("data"), "data 为 null 时应省略 data 键");
    }

    @Test
    void success静态方法应返回成功结果() {
        PccAjaxResult result = PccAjaxResult.success(200, "成功", "payload");
        assertEquals(200, result.get("code"));
        assertEquals("成功", result.get("msg"));
        assertEquals("payload", result.get("data"));
    }

    @Test
    void error静态方法应返回错误结果() {
        PccAjaxResult result = PccAjaxResult.error(401, "未登录", null);
        assertEquals(401, result.get("code"));
        assertEquals("未登录", result.get("msg"));
        assertFalse(result.containsKey("data"));
    }

    @Test
    void put应支持链式调用() {
        PccAjaxResult result = new PccAjaxResult()
                .put("code", 200)
                .put("extra", "自定义字段");
        assertEquals(200, result.get("code"));
        assertEquals("自定义字段", result.get("extra"));
        assertSame(result, result.put("x", 1), "put 应返回自身以支持链式");
    }
}
