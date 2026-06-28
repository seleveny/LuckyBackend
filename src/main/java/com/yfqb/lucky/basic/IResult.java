package com.yfqb.lucky.basic;

import com.yfqb.lucky.utils.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Setter
@Getter
public final class IResult<T> {
  private int code;
  private T result;
  private String message;

  private IResult(int code, T result, String message) {
    this.code = code;
    this.result = result;
    this.message = message;
  }

  public static <T> Mono<IResult<T>> success(T result) {
    return success(result, "成功");
  }

  public static <T> Mono<IResult<T>> success(T result, String message) {
    return Mono.just(new IResult<>(200, result, message));
  }

  public static <T> Mono<IResult<T>> error(String message) {
    return Mono.just(new IResult<>(444, null, message));
  }

  @Override
  public String toString() {
    return result == null ? "" : JsonUtil.toJSONString(result);
  }

}
