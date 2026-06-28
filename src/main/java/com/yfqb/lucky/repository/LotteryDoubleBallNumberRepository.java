package com.yfqb.lucky.repository;

import com.yfqb.lucky.model.po.LotteryDoubleBallNumber;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 双色球号码明细 Repository
 * <p>
 * 用于高效查询某个号码在最近 N 期中的出现次数。
 */
@Repository
public interface LotteryDoubleBallNumberRepository extends R2dbcRepository<LotteryDoubleBallNumber, Long> {

    /**
     * 查询某号码在指定日期范围内的出现次数
     */
    Mono<Integer> countByNumberAndTypeAndDrawDateBetween(Integer number, Integer type, LocalDate start, LocalDate end);

    /**
     * 查询某号码最近一次出现的记录
     */
    Mono<LotteryDoubleBallNumber> findTopByNumberAndTypeOrderByDrawDateDesc(Integer number, Integer type);

    /**
     * 查询某号码在指定日期范围内的明细
     */
    Flux<LotteryDoubleBallNumber> findByNumberAndTypeAndDrawDateBetweenOrderByDrawDateAsc(Integer number, Integer type, LocalDate start, LocalDate end);

    /**
     * 查询某期所有号码明细
     */
    Flux<LotteryDoubleBallNumber> findByPeriod(String period);

    /**
     * 删除某期的号码明细
     */
    Mono<Integer> deleteByPeriod(String period);
}
