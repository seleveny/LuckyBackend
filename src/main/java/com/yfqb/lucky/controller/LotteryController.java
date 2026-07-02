package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 彩票通用控制器
 * <p>
 * 提供彩票类型列表等通用查询接口。
 * 各类彩票的详细查询请使用对应的专用 Controller：
 * <ul>
 *   <li>{@link WelfareLotteryController} — 福利彩票（双色球、福彩3D、七乐彩）</li>
 *   <li>{@link SportLotteryController} — 体育彩票（大乐透、排列3、排列5、七星彩）</li>
 * </ul>
 *
 * @author xuchengcheng
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @GetMapping("/types")
    public Mono<IResult<String>> types() {
        return IResult.success("福利彩票: 双色球(ssq)、福彩3D(fc3d)、七乐彩(qlc)；体育彩票: 大乐透(dlt)、排列3(pl3)、排列5(pl5)、七星彩(qxc)");
    }
}
