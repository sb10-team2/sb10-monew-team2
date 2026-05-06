package com.springboot.monew.user.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityOutboxPayloadSerializerTest {

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private UserActivityOutboxPayloadSerializer userActivityOutboxPayloadSerializer;

  @Test
  @DisplayName("payload 객체를 JSON 문자열로 직렬화한다")
  void toJson_success() throws Exception {
    // given
    Map<String, Object> payload = Map.of("event", "payload");
    String payloadJson = "{\"event\":\"payload\"}";

    // ObjectMapper가 payload를 JSON 문자열로 직렬화한 결과를 미리 준비한다.
    given(objectMapper.writeValueAsString(payload)).willReturn(payloadJson);

    // when
    String result = userActivityOutboxPayloadSerializer.toJson(payload);

    // then
    assertThat(result).isEqualTo(payloadJson);
  }

  @Test
  @DisplayName("payload 직렬화에 실패하면 IllegalStateException으로 변환한다")
  void toJson_throwsIllegalStateException_whenSerializationFails() throws Exception {
    // given
    Map<String, Object> payload = Map.of("event", "payload");

    // ObjectMapper 직렬화 실패를 가정한다.
    given(objectMapper.writeValueAsString(payload))
        .willThrow(new JsonProcessingException("serialization failed") {
        });

    // when & then
    assertThatThrownBy(() -> userActivityOutboxPayloadSerializer.toJson(payload))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("사용자 활동 Outbox payload 직렬화에 실패했습니다.");
  }

  @Test
  @DisplayName("payload 역직렬화에 실패하면 IllegalStateException으로 변환한다")
  void fromJson_throwsIllegalStateException_whenDeserializationFails() throws Exception {
    // given
    String payloadJson = "invalid-json";

    // ObjectMapper 역직렬화 실패를 가정한다.
    given(objectMapper.readValue(payloadJson, Map.class))
        .willThrow(new JsonProcessingException("deserialization failed") {
        });

    // when & then
    assertThatThrownBy(() -> userActivityOutboxPayloadSerializer.fromJson(payloadJson, Map.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("사용자 활동 Outbox payload 역직렬화에 실패했습니다.");
  }
}
