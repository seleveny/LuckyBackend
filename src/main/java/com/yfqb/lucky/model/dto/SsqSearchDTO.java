package com.yfqb.lucky.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 双色球搜索入参 DTO
 * <p>
 * 支持按红球列表、蓝球筛选，以及分页查询。
 * 红球和蓝球均为可选参数；不传时默认返回最新 100 条。
 * 传了红球列表时，返回同时包含所有指定红球号码的开奖记录。
 */
@Data
public class SsqSearchDTO {

    /** 页码，从 1 开始，默认 1 */
    private Integer page = 1;

    /** 每页条数，默认 100 */
    private Integer pageSize = 100;

    /** 筛选的红球号码列表 (1-33)，可选。同时包含所有指定红球的期号才会被返回 */
    private List<Integer> redBalls;

    /** 筛选的蓝球号码 (1-16)，可选 */
    private Integer blueBall;
}
