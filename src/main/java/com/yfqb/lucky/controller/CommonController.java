package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author xuchengcheng
 * @since 2026-04-23
 *
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    @GetMapping("/ok")
    public Mono<IResult<String>> ok() {
        return IResult.success("hello world");
    }
}
