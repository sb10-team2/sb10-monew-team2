package com.springboot.monew.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import com.springboot.monew.notification.service.NotificationService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private NotificationService service;
  private final String endpoint = "/api/notifications";
  private final String userIdHeaderKey = "Monew-Request-User-ID";

  @ParameterizedTest(name = """
      cursor 조회 성공
      query parameter:[cursor, after, limit]
      header: [Monew-Request-User-ID]
      query로 전달 받은 값들은 모두 유효하다
      header로 userId를 전달 받는다
      params: {0}""")
  @MethodSource("provideQueryParamsAndUserId")
  void successToFind(Map<String, String> params) throws Exception {
    // given
    params = new HashMap<>(params);
    UUID userId = UUID.fromString(params.remove("userId"));
    String rawCursor = params.getOrDefault("cursor", null);
    UUID cursor = rawCursor == null ? null : UUID.fromString(rawCursor);
    String rawAfter = params.getOrDefault("after", null);
    Instant after = rawAfter == null ? null : Instant.parse(rawAfter);
    Integer limit = Integer.valueOf(params.get("limit"));
    MultiValueMap<String, String> queryParams = MultiValueMap.fromSingleValue(params);
    @SuppressWarnings("unchecked")
    CursorPageResponse<NotificationDto> result = Instancio.of(CursorPageResponse.class)
        .withTypeParameters(NotificationDto.class)
        .create();
    given(service.find(any(NotificationFindRequest.class), any(UUID.class))).willReturn(result);

    // when
    mockMvc.perform(
            get(resolveUri(""))
                .header(userIdHeaderKey, userId)
                .params(queryParams)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(print());

    // then
    ArgumentCaptor<NotificationFindRequest> requestCaptor = ArgumentCaptor.forClass(
        NotificationFindRequest.class);
    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(service, times(1)).find(requestCaptor.capture(), userIdCaptor.capture());
    Assertions.assertThat(userIdCaptor.getValue()).isEqualTo(userId);
    Assertions.assertThat(requestCaptor.getValue())
        .extracting("cursor", "after", "limit")
        .contains(cursor, after, limit);
  }

  @ParameterizedTest(name = """
      [cursor, after]: optional, [limit, Monew-Request-User-ID]: required
      필수 parameter가 없으면 exception 발생한다
      {0}""")
  @MethodSource("provideInvalidQueryParamsAndUserId")
  void failToFind(Map<String, String> params, Class<?> exception, ResultMatcher status)
      throws Exception {
    params = new HashMap<>(params);
    String rawUserId = params.remove("userId");
    UUID userId = rawUserId == null ? null : UUID.fromString(rawUserId);
    MultiValueMap<String, String> queryParams = MultiValueMap.fromSingleValue(params);
    @SuppressWarnings("unchecked")
    CursorPageResponse<NotificationDto> mockResult = Instancio.of(CursorPageResponse.class)
        .withTypeParameters(NotificationDto.class)
        .create();
    given(service.find(any(NotificationFindRequest.class), any(UUID.class))).willReturn(mockResult);

    var requestBuilder = get(resolveUri(""))
        .params(queryParams)
        .contentType(MediaType.APPLICATION_JSON);
    if (userId != null) {
      requestBuilder.header(userIdHeaderKey, userId);
    }

    // when & then
    mockMvc.perform(requestBuilder)
        .andExpect(status)
        .andExpect(result -> assertThrowException(result, exception));
  }

  private void assertThrowException(MvcResult result,
      Class<?> exception) {
    Assertions.assertThat(result)
        .extracting("resolvedException")
        .isInstanceOf(exception);
  }

  @Test
  @DisplayName("path variable에서 알림Id, header에서 유저Id를 가져온다")
  void update() throws Exception {
    // given
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    willDoNothing().given(service).update(any(UUID.class), any(UUID.class));

    // when
    mockMvc.perform(
            patch(resolveUri("{notificationId}"), id)
                .header("Monew-Request-User-ID", userId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andDo(print());

    // then
    ArgumentCaptor<UUID> notificationIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(service, times(1)).update(notificationIdCaptor.capture(), userIdCaptor.capture());
    Assertions.assertThat(notificationIdCaptor.getValue()).isEqualTo(id);
    Assertions.assertThat(userIdCaptor.getValue()).isEqualTo(userId);
  }

  private String resolveUri(String resource) {
    if (resource.isEmpty()) {
      return endpoint;
    }
    return String.join("/", endpoint, resource);
  }

  private static Stream<Arguments> provideInvalidQueryParamsAndUserId() {
    return Stream.of(
        Arguments.of(Map.of(),
            MethodArgumentNotValidException.class,
            status().isBadRequest()),
        Arguments.of(Map.of("cursor", id(), "after", getStringDatetime()),
            MethodArgumentNotValidException.class,
            status().isBadRequest()),
        Arguments.of(Map.of("userId", id()),
            MethodArgumentNotValidException.class,
            status().isBadRequest()),
        Arguments.of(Map.of("limit", "10"),
            MissingRequestHeaderException.class,
            status().isUnauthorized())
    );
  }

  private static Stream<Map<String, String>> provideQueryParamsAndUserId() {
    return Stream.of(
        Map.of("limit", "10", "userId", id()),
        Map.of("after", getStringDatetime(), "limit", "3", "userId", id()),
        Map.of("cursor", id(), "after", getStringDatetime(), "limit", "5", "userId", id())
    );
  }

  private static String id() {
    return UUID.randomUUID().toString();
  }

  private static String getStringDatetime() {
    return Instant.now().toString();
  }
}
