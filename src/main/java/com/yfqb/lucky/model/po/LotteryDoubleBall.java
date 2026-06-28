package com.yfqb.lucky.model.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 双色球开奖记录 PO
 */
@Data
@Table("lottery_double_ball")
public class LotteryDoubleBall {

    @Id
    private Long id;

    /** 期号，如 2024068 */
    private String period;

    /** 开奖日期 */
    private LocalDate drawDate;

    /** 星期，如 星期二 */
    private String weekday;

    /** 红球1 (1-33) */
    private Integer redOne;

    /** 红球2 (1-33) */
    private Integer redTwo;

    /** 红球3 (1-33) */
    private Integer redThree;

    /** 红球4 (1-33) */
    private Integer redFour;

    /** 红球5 (1-33) */
    private Integer redFive;

    /** 红球6 (1-33) */
    private Integer redSix;

    /** 蓝球 (1-16) */
    private Integer blue;

    /** 奖池金额（元） */
    private BigDecimal poolAmount;

    /** 一等奖注数 */
    private Integer firstPrizeCount;

    /** 一等奖单注金额（元） */
    private BigDecimal firstPrizeAmount;

    /** 二等奖注数 */
    private Integer secondPrizeCount;

    /** 二等奖单注金额（元） */
    private BigDecimal secondPrizeAmount;

    /** 本期销售额（元） */
    private BigDecimal salesAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
