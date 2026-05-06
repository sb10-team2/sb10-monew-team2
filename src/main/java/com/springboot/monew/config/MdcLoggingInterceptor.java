package com.springboot.monew.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

public class MdcLoggingInterceptor implements HandlerInterceptor {

  /*
  - preHandle: 컨트롤러 실행 전 호출 → MDC 세팅
  - afterCompletion: 응답이 완전히 끝난 후 호출 → MDC 해제
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String requestId = UUID.randomUUID().toString();
    MDC.put("request_id", requestId);
    MDC.put("method", request.getMethod());
    MDC.put("uri", request.getRequestURI());
    MDC.put("ip", request.getRemoteAddr());
    response.setHeader("Monew-Request-ID", requestId);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    MDC.clear();
  }
}
