package com.yfqb.lucky.utils;


import com.yfqb.lucky.constant.CommonConstants;
import com.yfqb.lucky.model.dto.UserInfo;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CurrentPrincipal {

  private static final List<String> IP_HEADERS = Arrays.asList(
          "x-forwarded-for",
          "proxy-client-ip",
          "wl-proxy-client-ip",
          "http_client_ip",
          "http_x_forwarded_for",
          "x-real-ip"
  );

  public static Mono<UserInfo> currentUserInfo() {
    return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> {
                if (context.getAuthentication() == null){
                    return Mono.empty();
                }else{
                    return Mono.just(context.getAuthentication());
                }
            })
            .mapNotNull(auth -> (UserInfo) auth.getDetails())
            .defaultIfEmpty(new UserInfo());
  }

  public static UserInfo currentUserInfo(WebSession webSession) {
    return (UserInfo)
        Objects.requireNonNull((Objects.requireNonNull(webSession.<SecurityContextImpl>getAttribute(CommonConstants.SPRING_SECURITY_CONTEXT)))
                        .getAuthentication())
            .getDetails();
  }

  public static Mono<UserInfo> refreshUserInfo(UserInfo userInfo, WebSession webSession) {
      SecurityContextImpl securityContext =
        (SecurityContextImpl) webSession.getAttributes().get(CommonConstants.SPRING_SECURITY_CONTEXT);

      assert securityContext.getAuthentication() != null;
      ((AbstractAuthenticationToken) securityContext.getAuthentication()).setDetails(userInfo);

    // update when set attribute
    webSession.getAttributes().put(CommonConstants.SPRING_SECURITY_CONTEXT, securityContext);

    return webSession.save().then(Mono.just(userInfo));
  }

  /**
   * 获取用户真实IP地址
   * @param exchange ServerWebExchange对象
   * @return 用户真实IP地址
   */
  public static String getRealIp(ServerWebExchange exchange) {
    // 遍历常见的IP头信息
    for (String header : IP_HEADERS) {
      String ip = exchange.getRequest().getHeaders().getFirst(header);
      if (isValidIp(ip)) {
        // 如果包含多个IP，取第一个作为客户端IP
        return extractFirstIp(ip);
      }
    }

    // 如果所有头部都没有有效IP，则使用直接连接的远程地址
    if (exchange.getRequest().getRemoteAddress() != null) {
      return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    return "";
  }

  /**
   * 验证IP地址是否有效
   * @param ip IP地址字符串
   * @return 是否为有效IP
   */
  private static boolean isValidIp(String ip) {
    return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
  }

  /**
   * 从可能包含多个IP的字符串中提取第一个IP
   * @param ip 包含多个IP的字符串
   * @return 第一个IP地址
   */
  private static String extractFirstIp(String ip) {
    if (ip.contains(",")) {
      return ip.split(",")[0].trim();
    }
    return ip.trim();
  }
}
