package com.wwfinance.api.entity.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典树形 VO（父子级联）
 */
@Getter
@Setter
@Accessors(chain = true)
public class DictDTO {

    private Long id;

    /** 上级 id（0=根分类） */
    private Long parentId;

    /** 名称 */
    private String name;

    /** 字典项值 */
    private Integer value;

    /** 分类编码（仅父节点有，如 industry/education） */
    private String dictCode;

    /** 子级字典项 */
    private List<DictDTO> children = new ArrayList<>();
}
