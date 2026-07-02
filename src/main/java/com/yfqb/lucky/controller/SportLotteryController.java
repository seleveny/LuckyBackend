package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 体育彩票控制器
 * <p>
 * 提供大乐透（DLT）、排列3（PL3）、排列5（PL5）、七星彩（QXC）等体育彩票的查询接口。
 *
 * @author xuchengcheng
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/api/sport")
public class SportLotteryController {

    // ==================== 大乐透 ====================

    @GetMapping("/dlt/latest")
    public Mono<IResult<String>> dltLatest() {
        return IResult.success("大乐透最新开奖");
    }

    @GetMapping("/dlt/history")
    public Mono<IResult<String>> dltHistory() {
        return IResult.success("大乐透历史开奖");
    }

    @GetMapping("/dlt/period")
    public Mono<IResult<String>> dltByPeriod() {
        return IResult.success("大乐透按期号查询");
    }

    // ==================== 排列3 ====================

    @GetMapping("/pl3/latest")
    public Mono<IResult<String>> pl3Latest() {
        return IResult.success("排列3最新开奖");
    }

    @GetMapping("/pl3/history")
    public Mono<IResult<String>> pl3History() {
        return IResult.success("排列3历史开奖");
    }

    // ==================== 排列5 ====================

    @GetMapping("/pl5/latest")
    public Mono<IResult<String>> pl5Latest() {
        return IResult.success("排列5最新开奖");
    }

    @GetMapping("/pl5/history")
    public Mono<IResult<String>> pl5History() {
        return IResult.success("排列5历史开奖");
    }

    // ==================== 七星彩 ====================

    @GetMapping("/qxc/latest")
    public Mono<IResult<String>> qxcLatest() {
        return IResult.success("七星彩最新开奖");
    }

    @GetMapping("/qxc/history")
    public Mono<IResult<String>> qxcHistory() {
        return IResult.success("七星彩历史开奖");
    }
}
