package com.yfqb.lucky.controller;

import com.yfqb.lucky.basic.IResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author xuchengcheng
 * @since 2026-04-23
 *
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @RequestMapping("/register")
    public Mono<IResult<String>> register() {
        return IResult.success("注册成功");
    }

    @RequestMapping("/login")
    public Mono<IResult<String>> login() {
        return IResult.success("登录成功");
    }

    @RequestMapping("/logout")
    public Mono<IResult<String>> logout() {
        return IResult.success("登出成功");
    }
}
