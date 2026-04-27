package com.springboot.monew.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.dto.response.SubscriptionDto;
import com.springboot.monew.interest.entity.InterestDirection;
import com.springboot.monew.interest.entity.InterestOrderBy;
import com.springboot.monew.interest.service.InterestService;
import com.springboot.monew.interest.service.SubscriptionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @MockitoBean
  private SubscriptionService subscriptionService;

  @Test
  @DisplayName("관심사 목록을 조회할 수 있다")
  void list_ReturnsInterestPage_WhenValidRequest() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        "eco",
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    );
    InterestDto interestDto = new InterestDto(UUID.randomUUID(), "economy", List.of("macro"), 3L, true);
    CursorPageResponseInterestDto response = new CursorPageResponseInterestDto(
        List.of(interestDto),
        null,
        null,
        1,
        1L,
        false
    );

    given(interestService.list(request, userId)).willReturn(response);

    // when
    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", userId)
            .param("keyword", "eco")
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "10"))
        // then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("economy"))
        .andExpect(jsonPath("$.content[0].keywords[0]").value("macro"))
        .andExpect(jsonPath("$.content[0].subscriberCount").value(3))
        .andExpect(jsonPath("$.content[0].subscribedByMe").value(true))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));

    verify(interestService).list(request, userId);
  }

  @Test
  @DisplayName("사용자 요청 헤더가 없으면 관심사 목록 조회에 실패한다")
  void list_ReturnsUnauthorized_WhenRequestUserIdHeaderIsMissing() throws Exception {
    // given
    String orderBy = "name";
    String direction = "ASC";
    String limit = "10";

    // when
    mockMvc.perform(get("/api/interests")
            .param("orderBy", orderBy)
            .param("direction", direction)
            .param("limit", limit))
        // then
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    verify(interestService, never()).list(any(), any());
  }

  @Test
  @DisplayName("limit가 0이면 관심사 목록 조회에 실패한다")
  void list_ReturnsBadRequest_WhenLimitIsZero() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    String orderBy = "name";
    String direction = "ASC";
    String limit = "0";

    // when
    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", userId)
            .param("orderBy", orderBy)
            .param("direction", direction)
            .param("limit", limit))
        // then
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.limit").isArray());

    verify(interestService, never()).list(any(), any());
  }

  @Test
  @DisplayName("limit가 최대 허용값을 초과하면 관심사 목록 조회에 실패한다")
  void list_ReturnsBadRequest_WhenLimitExceedsMaximum() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    String orderBy = "name";
    String direction = "ASC";
    String limit = "101";

    // when
    mockMvc.perform(get("/api/interests")
            .header("Monew-Request-User-ID", userId)
            .param("orderBy", orderBy)
            .param("direction", direction)
            .param("limit", limit))
        // then
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.limit").isArray());

    verify(interestService, never()).list(any(), any());
  }

  @Test
  @DisplayName("관심사를 생성할 수 있다")
  void create_ReturnsCreatedInterest_WhenValidRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    InterestRegisterRequest request = new InterestRegisterRequest("금융", List.of("주식", "채권"));
    InterestDto response = new InterestDto(interestId, "금융", List.of("주식", "채권"), 0L, false);

    given(interestService.create(any(InterestRegisterRequest.class))).willReturn(response);

    // when
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        // then
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/interests/" + interestId))
        .andExpect(jsonPath("$.id").value(interestId.toString()))
        .andExpect(jsonPath("$.name").value("금융"))
        .andExpect(jsonPath("$.keywords[0]").value("주식"));

    verify(interestService).create(any(InterestRegisterRequest.class));
  }

  @Test
  @DisplayName("관심사 이름이 비어 있으면 생성에 실패한다")
  void create_ReturnsBadRequest_WhenNameIsBlank() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("", List.of("주식"));

    // when
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        // then
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.name").isArray());

    verify(interestService, never()).create(any());
  }

  @Test
  @DisplayName("키워드가 비어 있으면 생성에 실패한다")
  void create_ReturnsBadRequest_WhenKeywordsAreEmpty() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("금융", List.of());

    // when
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        // then
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.keywords").isArray());

    verify(interestService, never()).create(any());
  }

  @Test
  @DisplayName("관심사를 수정할 수 있다")
  void update_ReturnsUpdatedInterest_WhenValidRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("ETF", "채권"));
    InterestDto response = new InterestDto(interestId, "금융", List.of("ETF", "채권"), 7L, false);

    given(interestService.update(eq(interestId), any(InterestUpdateRequest.class))).willReturn(response);

    // when
    mockMvc.perform(patch("/api/interests/{interestId}", interestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        // then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(interestId.toString()))
        .andExpect(jsonPath("$.subscriberCount").value(7));

    verify(interestService).update(eq(interestId), any(InterestUpdateRequest.class));
  }

  @Test
  @DisplayName("수정 요청의 키워드가 비어 있으면 수정에 실패한다")
  void update_ReturnsBadRequest_WhenKeywordsAreEmpty() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    InterestUpdateRequest request = new InterestUpdateRequest(List.of());

    // when
    mockMvc.perform(patch("/api/interests/{interestId}", interestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        // then
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.keywords").isArray());

    verify(interestService, never()).update(eq(interestId), any());
  }

  @Test
  @DisplayName("관심사를 삭제할 수 있다")
  void delete_ReturnsNoContent_WhenInterestExists() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when
    mockMvc.perform(delete("/api/interests/{interestId}", interestId))
        // then
        .andExpect(status().isNoContent());

    verify(interestService).delete(interestId);
  }

  @Test
  @DisplayName("관심사를 구독할 수 있다")
  void subscribe_ReturnsSubscriptionDto_WhenValidRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    SubscriptionDto response = new SubscriptionDto(
        UUID.randomUUID(),
        interestId,
        "금융",
        List.of("주식"),
        3L,
        Instant.parse("2026-04-20T00:00:00Z")
    );

    given(subscriptionService.subscribe(interestId, userId)).willReturn(response);

    // when
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header("Monew-Request-User-ID", userId))
        // then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interestId").value(interestId.toString()))
        .andExpect(jsonPath("$.interestName").value("금융"))
        .andExpect(jsonPath("$.interestSubscriberCount").value(3));

    verify(subscriptionService).subscribe(interestId, userId);
  }

  @Test
  @DisplayName("사용자 요청 헤더가 없으면 관심사 구독에 실패한다")
  void subscribe_ReturnsUnauthorized_WhenRequestUserIdHeaderIsMissing() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId))
        // then
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    verify(subscriptionService, never()).subscribe(any(), any());
  }

  @Test
  @DisplayName("관심사 구독을 취소할 수 있다")
  void unsubscribe_ReturnsNoContent_WhenValidRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header("Monew-Request-User-ID", userId))
        // then
        .andExpect(status().isNoContent());

    verify(subscriptionService).unsubscribe(interestId, userId);
  }

  @Test
  @DisplayName("사용자 요청 헤더가 없으면 관심사 구독 취소에 실패한다")
  void unsubscribe_ReturnsUnauthorized_WhenRequestUserIdHeaderIsMissing() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId))
        // then
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    verify(subscriptionService, never()).unsubscribe(any(), any());
  }
}
