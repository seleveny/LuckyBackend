package com.yfqb.lucky.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 通用分页结果 VO
 *
 * @param <T> 列表元素类型
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int pageSize;

    /** 总条数 */
    private long total;

    /** 数据列表 */
    private List<T> list;
}
