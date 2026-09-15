package com.wwfinance.api.entity.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据字典 Excel 导入导出 DTO
 * 导出：父节点（分类）只有名称+编码，子节点（字典项）带编码值
 */
@Getter
@Setter
public class ExcelDictDTO {

    @ExcelProperty("字典编码")
    private String dictCode;

    @ExcelProperty("字典项名称")
    private String name;

    @ExcelProperty("字典项值")
    private Integer value;

    @ExcelProperty("上级编码")
    private String parentDictCode;
}
