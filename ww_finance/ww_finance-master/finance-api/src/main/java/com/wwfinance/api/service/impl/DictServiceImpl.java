package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.Dict;
import com.wwfinance.api.entity.dto.DictDTO;
import com.wwfinance.api.entity.dto.ExcelDictDTO;
import com.wwfinance.api.mapper.DictMapper;
import com.wwfinance.api.service.DictService;
import com.wwfinance.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典服务实现：树形结构、父子级联、Excel 导入导出
 */
@Slf4j
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    /** 根节点 id */
    private static final long ROOT_ID = 1L;

    @Override
    public List<Dict> listByDictCode(String dictCode) {
        if (dictCode == null || dictCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // 先按编码找到分类节点
        Dict parent = getOne(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictCode, dictCode.trim())
                .eq(Dict::getDeleted, false)
                .last("limit 1"));
        if (parent == null) {
            return new ArrayList<>();
        }
        return list(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getParentId, parent.getId())
                .eq(Dict::getDeleted, false)
                .orderByAsc(Dict::getValue));
    }

    @Override
    public List<DictDTO> listTree() {
        List<Dict> all = list(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getDeleted, false)
                .orderByAsc(Dict::getId));
        Map<Long, DictDTO> nodeMap = new HashMap<>();
        // 第一遍：转 DTO 并按 id 建索引
        for (Dict d : all) {
            DictDTO dto = new DictDTO()
                    .setId(d.getId())
                    .setParentId(d.getParentId())
                    .setName(d.getName())
                    .setValue(d.getValue())
                    .setDictCode(d.getDictCode());
            nodeMap.put(d.getId(), dto);
        }
        // 第二遍：挂父子关系
        List<DictDTO> roots = new ArrayList<>();
        for (Dict d : all) {
            DictDTO dto = nodeMap.get(d.getId());
            DictDTO parent = nodeMap.get(d.getParentId());
            if (parent == null) {
                // 没有父节点（含根 ROOT）作为顶级
                roots.add(dto);
            } else {
                parent.getChildren().add(dto);
            }
        }
        return roots;
    }

    @Override
    public void add(Long parentId, String name, Integer value, String dictCode) {
        if (parentId == null || name == null || name.trim().isEmpty()) {
            throw new BusinessException("上级节点与名称不能为空");
        }
        Dict parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException("上级节点不存在");
        }
        // 父节点是分类时，子节点不再带 dictCode；父节点是根时允许填分类编码
        String code = (parent.getParentId() == null || parent.getParentId() == 0L)
                ? dictCode : null;
        Dict dict = new Dict()
                .setParentId(parentId)
                .setName(name.trim())
                .setValue(value)
                .setDictCode(code)
                .setDeleted(false);
        save(dict);
        log.info("新增字典: id={}, parentId={}, name={}, dictCode={}", dict.getId(), parentId, name, code);
    }

    @Override
    public void update(Long id, Long parentId, String name, Integer value, String dictCode) {
        Dict dict = getById(id);
        if (dict == null) {
            throw new BusinessException("字典不存在");
        }
        if (parentId != null) {
            dict.setParentId(parentId);
        }
        if (name != null && !name.trim().isEmpty()) {
            dict.setName(name.trim());
        }
        if (value != null) {
            dict.setValue(value);
        }
        if (dictCode != null && !dictCode.trim().isEmpty()) {
            dict.setDictCode(dictCode.trim());
        }
        updateById(dict);
        log.info("修改字典: id={}", id);
    }

    @Override
    public void delete(Long id) {
        long childCount = count(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getParentId, id)
                .eq(Dict::getDeleted, false));
        if (childCount > 0) {
            throw new BusinessException("该字典存在子节点，不能删除");
        }
        Dict dict = getById(id);
        if (dict != null) {
            dict.setDeleted(true);
            updateById(dict);
        }
        log.info("删除字典: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(List<ExcelDictDTO> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int count = 0;
        // 编码 -> 分类节点 id 缓存（父级先行，Excel 中分类行应在子项之前）
        Map<String, Long> codeIdMap = new HashMap<>();
        for (ExcelDictDTO row : list) {
            if (row.getName() == null || row.getName().trim().isEmpty()) {
                continue;
            }
            String code = trim(row.getDictCode());
            String parentCode = trim(row.getParentDictCode());
            if (parentCode == null || parentCode.isEmpty()) {
                // 分类行：dictCode 非空，挂到根下
                Dict exist = getOne(new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getDictCode, code)
                        .eq(Dict::getDeleted, false)
                        .last("limit 1"));
                if (exist == null) {
                    Dict parent = new Dict()
                            .setParentId(ROOT_ID)
                            .setName(row.getName().trim())
                            .setDictCode(code)
                            .setDeleted(false);
                    save(parent);
                    codeIdMap.put(code, parent.getId());
                    count++;
                } else {
                    codeIdMap.put(code, exist.getId());
                }
            } else {
                // 字典项行：挂到父分类下
                Long parentId = codeIdMap.get(parentCode);
                if (parentId == null) {
                    Dict p = getOne(new LambdaQueryWrapper<Dict>()
                            .eq(Dict::getDictCode, parentCode)
                            .eq(Dict::getDeleted, false)
                            .last("limit 1"));
                    if (p == null) {
                        log.warn("导入跳过：上级编码不存在 parentCode={}, name={}", parentCode, row.getName());
                        continue;
                    }
                    parentId = p.getId();
                    codeIdMap.put(parentCode, parentId);
                }
                Dict exist = getOne(new LambdaQueryWrapper<Dict>()
                        .eq(Dict::getParentId, parentId)
                        .eq(Dict::getName, row.getName().trim())
                        .eq(Dict::getDeleted, false)
                        .last("limit 1"));
                if (exist == null) {
                    Dict item = new Dict()
                            .setParentId(parentId)
                            .setName(row.getName().trim())
                            .setValue(row.getValue())
                            .setDeleted(false);
                    save(item);
                    count++;
                } else if (row.getValue() != null) {
                    exist.setValue(row.getValue());
                    updateById(exist);
                }
            }
        }
        log.info("字典 Excel 导入完成: 处理 {} 行", count);
        return count;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
