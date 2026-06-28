package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 彩票控制器
 *
 * @author xuchengcheng
 * @since 2026-04-23
 */
@RestController
@RequestMapping("/lottery")
public class LotteryController {

    @RequestMapping("/list")
    public Mono<IResult<String>> list() {
        return IResult.success("彩票列表");
    }

    @RequestMapping("/detail")
    public Mono<IResult<String>> detail() {
        return IResult.success("彩票详情");
    }
}
