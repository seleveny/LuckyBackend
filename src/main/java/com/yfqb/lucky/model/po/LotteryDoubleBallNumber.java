package com.yfqb.lucky.model.po;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 双色球号码明细 PO
 * <p>
 * 每期 7 条记录：6 个红球 + 1 个蓝球，用于高效查询号码出现频率。
 */
@Data
@Table("lottery_double_ball_number")
public class LotteryDoubleBallNumber {

    @Id
    private Long id;

    /** 期号 */
    private String period;

    /** 开奖日期 */
    private LocalDate drawDate;

    /** 号码 (1-33 红球, 1-16 蓝球) */
    private Integer number;

    /** 类型：1-红球，2-蓝球 */
    private Integer type;

    /** 创建时间 */
    private LocalDateTime createTime;
}
