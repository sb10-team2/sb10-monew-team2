package com.springboot.monew.user.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActivityOutboxPayloadSerializer {

  private final ObjectMapper objectMapper;

  // payload 객체를 JSON 문자열로 변환한다.
  public String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("사용자 활동 Outbox payload 직렬화에 실패했습니다.", e);
    }
  }

  // JSON 문자열을 payload 객체로 복원한다.
  public <T> T fromJson(String payloadJson, Class<T> payloadType) {
    try {
      return objectMapper.readValue(payloadJson, payloadType);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("사용자 활동 Outbox payload 역직렬화에 실패했습니다.", e);
    }
  }
}
