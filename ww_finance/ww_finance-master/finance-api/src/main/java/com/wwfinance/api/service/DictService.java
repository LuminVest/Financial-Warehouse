package com.wwfinance.api.service;

import com.wwfinance.api.entity.Dict;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wwfinance.api.entity.dto.DictDTO;

import java.util.List;

/**
 * 数据字典服务：树形查询 + 按编码取字典项 + Excel 导入导出
 */
public interface DictService extends IService<Dict> {

    /**
     * 按分类编码查询字典项列表（用于下拉选择，如 industry/education）
     */
    List<Dict> listByDictCode(String dictCode);

    /**
     * 树形结构（全部，管理端用）
     */
    List<DictDTO> listTree();

    /**
     * 管理端：新增
     */
    void add(Long parentId, String name, Integer value, String dictCode);

    /**
     * 管理端：修改
     */
    void update(Long id, Long parentId, String name, Integer value, String dictCode);

    /**
     * 管理端：删除（有子节点不允许删除）
     */
    void delete(Long id);

    /**
     * Excel 导入（按上级编码/分类编码 upsert）
     */
    int importExcel(List<com.wwfinance.api.entity.dto.ExcelDictDTO> list);
}
