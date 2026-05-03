package com.springboot.monew.newsarticles.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.dto.response.RestoreResultDto;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.metric.result.NewsArticleCollectResult;
import com.springboot.monew.newsarticles.s3.NewsArticleRestoreService;
import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

//Controller 계층만 로딩
@WebMvcTest(NewsArticleController.class)
class NewsArticleControllerTest {

  @Autowired
  private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하기 위한 객체

  // Controller에서 사용하는 Service는 실제 Bean이 아니라 Mock으로 대체
  @MockitoBean
  private NewsArticleCollectService newsArticleCollectService;

  @MockitoBean
  private NewsArticleService newsArticleService;

  @MockitoBean
  private NewsArticleRestoreService newsArticleRestoreService;

  @Test
  @DisplayName("뉴스기사 수집 API - 정상 요청시 200 OK 반환 ")
  void collectNews_ReturnsOk_WhenRequestIsValid() throws Exception {

    // given

    // when & then
    mockMvc.perform(post("/api/articles"))
        .andExpect(status().isOk())
        .andExpect(content().string("뉴스 수집 완료"));

    verify(newsArticleCollectService).collectAll();
  }

  @Test
  @DisplayName("뉴스 수집 API - 서비스 예외 발생 시 500 반환")
  void collectNews_Fail_WhenServiceThrowsException() throws Exception {

    // given
    // 서비스 계층에서 RuntimeException이 발생하는 상황을 가정.
    doThrow(new RuntimeException("수집 실패"))
        .when(newsArticleCollectService).collectAll();

    // when & then
    // 서비스 예외로 인해 HTTP 500 상태가 반환되는지 검증
    mockMvc.perform(post("/api/articles"))
        .andExpect(status().isInternalServerError());

    // 호출은 되었는지 확인
    verify(newsArticleCollectService, times(1)).collectAll();
  }

  @Test
  @DisplayName("뉴스 수집 API - GET 요청 시 400 Bad Request (list API로 매핑됨)")
  void collectNews_GetRequest_ReturnsBadRequest() throws Exception {

    mockMvc.perform(get("/api/articles"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("기사 조회 생성 API - 정상 요청 시 201 Created + Location 헤더 반환")
  void createView_ReturnsCreated_WhenRequestIsValid() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID viewId = UUID.randomUUID();

    // 서비스가 반환할 DTO Mock
    NewsArticleViewDto dto = new NewsArticleViewDto(
        viewId,
        userId,
        Instant.now(),
        articleId,
        ArticleSource.NAVER,
        "https://news.url",
        "테스트 기사 제목",
        Instant.now(),
        "요약 내용",
        10L,
        100L
    );

    when(newsArticleService.createView(articleId, userId)).thenReturn(dto);

    // when & then
    mockMvc.perform(
            post("/api/articles/{articleId}/article-views", articleId)
                .header("Monew-Request-User-ID", userId.toString())
        )
        // 상태코드 검증
        .andExpect(status().isCreated())

        // Location 헤더 검증 (핵심)
        .andExpect(header().string("Location",
            "/api/articles/" + articleId + "/article-views/" + viewId))

        // 응답 JSON 검증 (id 필드만 핵심 체크)
        .andExpect(jsonPath("$.id").value(viewId.toString()));

    // 서비스 호출 검증 (위임 책임)
    verify(newsArticleService, times(1)).createView(articleId, userId);
  }

  @Test
  @DisplayName("기사 조회 생성 API - 사용자 헤더 누락 시 401 Unauthorized")
  void createView_ReturnsUnauthorized_WhenUserHeaderMissing() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    // 필수 사용자 헤더가 없으면 MissingRequestHeaderException이 발생하고,
    // 현재 GlobalExceptionHandler 정책에 따라 401 Unauthorized가 반환된다.
    mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.exceptionType").value("MissingRequestHeaderException"))
        .andExpect(jsonPath("$.status").value(401));

    // 헤더 바인딩 단계에서 실패하므로 Service는 호출되지 않는다.
    verify(newsArticleService, never()).createView(any(), any());
  }

  @Test
  @DisplayName("기사 조회 생성 API - 사용자 헤더 UUID 형식 오류 시 500")
  void createView_ReturnsInternalServerError_WhenUserHeaderIsInvalid() throws Exception {

    UUID articleId = UUID.randomUUID();

    mockMvc.perform(
            post("/api/articles/{articleId}/article-views", articleId)
                .header("Monew-Request-User-ID", "invalid-user-id")
        )
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.exceptionType").value("MethodArgumentTypeMismatchException"));

    verify(newsArticleService, never()).createView(any(), any());
  }

  @Test
  @DisplayName("기사 조회 생성 API - articleId UUID 형식이 잘못되면 500 Internal Server Error")
  void createView_ReturnsInternalServerError_WhenArticleIdIsInvalid() throws Exception {

    // given
    String invalidArticleId = "not-a-uuid";
    UUID userId = UUID.randomUUID();

    // when & then
    // articleId는 UUID 타입이므로 잘못된 문자열이 들어오면
    // MethodArgumentTypeMismatchException이 발생한다.
    // 현재 GlobalExceptionHandler 정책상 해당 예외는 500으로 처리된다.
    mockMvc.perform(
            post("/api/articles/{articleId}/article-views", invalidArticleId)
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.exceptionType").value("MethodArgumentTypeMismatchException"))
        .andExpect(jsonPath("$.status").value(500));

    // PathVariable 변환 단계에서 실패하므로 Service는 호출되지 않는다.
    verify(newsArticleService, never()).createView(any(), any());
  }



  @Test
  @DisplayName("뉴스 기사 목록 조회 - 정상 요청 시 200 OK + 페이지 응답 반환")
  void list_ReturnsPageResponse_WhenRequestIsValid() throws Exception {

    // given
    UUID userId = UUID.randomUUID();

    // 서비스가 반환할 더미 응답 객체 생성
    CursorPageResponseNewsArticleDto response =
        new CursorPageResponseNewsArticleDto(
            List.of(),   // content
            "nextCursor",
            null,        // nextAfter
            5,           // size
            10L,         // totalElements
            true         // hasNext
        );

    // 서비스 호출 시 위 응답을 반환하도록 설정
    when(newsArticleService.list(any(), eq(userId))).thenReturn(response);

    // when & then
    // 필수 요청 파라미터(limit, orderBy, direction)를 포함하여 GET 요청 수행
    mockMvc.perform(
            get("/api/articles")
                .param("limit", "5")
                .param("orderBy", "publishDate")
                .param("direction", "DESC")
                .header("Monew-Request-User-ID", userId.toString())
        )
        // 정상 응답 → 200 OK
        .andExpect(status().isOk())

        // JSON 응답 검증 (핵심 필드만)
        .andExpect(jsonPath("$.nextCursor").value("nextCursor"))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.hasNext").value(true));

    // then
    // 컨트롤러가 서비스에 요청을 위임했는지 검증
    verify(newsArticleService, times(1)).list(any(), eq(userId));
  }

  @Test
  @DisplayName("뉴스 기사 목록 조회 - 필수 파라미터 누락 시 400 Bad Request")
  void list_ReturnsBadRequest_WhenRequiredParamMissing() throws Exception {

    UUID userId = UUID.randomUUID();

    // when & then
    // limit/orderBy/direction 없이 요청 → @Valid 검증 실패
    mockMvc.perform(
            get("/api/articles")
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isBadRequest());

    // 검증 실패이므로 서비스 호출되지 않아야 함
    verify(newsArticleService, never()).list(any(), any());
  }

  @Test
  @DisplayName("뉴스 기사 목록 조회 - 사용자 헤더 누락 시 401 Unauthorized")
  void list_ReturnsUnauthorized_WhenHeaderMissing() throws Exception {

    mockMvc.perform(
            get("/api/articles")
                .param("limit", "5")
                .param("orderBy", "publishDate")
                .param("direction", "DESC")
        )
        .andExpect(status().isUnauthorized());

    verify(newsArticleService, never()).list(any(), any());
  }

  @Test
  @DisplayName("뉴스 기사 목록 조회 - orderBy 값이 잘못되면 400 Bad Request")
  void list_ReturnsBadRequest_WhenOrderByIsInvalid() throws Exception {

    UUID userId = UUID.randomUUID();

    mockMvc.perform(
            get("/api/articles")
                .param("limit", "5")
                .param("orderBy", "invalid")
                .param("direction", "DESC")
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.exceptionType").value("MethodArgumentNotValidException"))
        .andExpect(jsonPath("$.details.orderBy").exists())
        .andExpect(jsonPath("$.status").value(400));

    verify(newsArticleService, never()).list(any(), any());
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 - 정상 요청 시 200 OK + DTO 반환")
  void find_ReturnsArticle_WhenRequestIsValid() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    NewsArticleDto dto = new NewsArticleDto(
        articleId,
        ArticleSource.NAVER,
        "https://news.url",
        "기사 제목",
        Instant.now(),
        "요약",
        5L,
        100L,
        false
    );

    when(newsArticleService.findById(articleId, userId)).thenReturn(dto);

    // when & then
    mockMvc.perform(
            get("/api/articles/{articleId}", articleId)
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(articleId.toString()))
        .andExpect(jsonPath("$.source").value("NAVER"))
        .andExpect(jsonPath("$.sourceUrl").value("https://news.url"))
        .andExpect(jsonPath("$.title").value("기사 제목"))
        .andExpect(jsonPath("$.summary").value("요약"))
        .andExpect(jsonPath("$.commentCount").value(5))
        .andExpect(jsonPath("$.viewCount").value(100))
        .andExpect(jsonPath("$.viewedByMe").value(false));

    verify(newsArticleService, times(1)).findById(articleId, userId);
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 - 서비스 예외 발생 시 500")
  void find_ReturnsInternalServerError_WhenServiceThrows() throws Exception {

    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    doThrow(new RuntimeException("조회 실패"))
        .when(newsArticleService).findById(articleId, userId);

    mockMvc.perform(
            get("/api/articles/{articleId}", articleId)
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isInternalServerError());

    verify(newsArticleService).findById(articleId, userId);
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 - articleId UUID 형식 오류 시 500")
  void find_ReturnsError_WhenArticleIdInvalid() throws Exception {

    UUID userId = UUID.randomUUID();

    mockMvc.perform(
            get("/api/articles/{articleId}", "invalid-uuid")
                .header("Monew-Request-User-ID", userId.toString())
        )
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.exceptionType")
            .value("MethodArgumentTypeMismatchException"));

    verify(newsArticleService, never()).findById(any(), any());
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 - 사용자 헤더 누락 시 401 Unauthorized")
  void find_ReturnsUnauthorized_WhenHeaderMissing() throws Exception {

    UUID articleId = UUID.randomUUID();

    mockMvc.perform(
            get("/api/articles/{articleId}", articleId)
        )
        .andExpect(status().isUnauthorized());

    // 바인딩 단계에서 실패 → 서비스 호출 X
    verify(newsArticleService, never()).findById(any(), any());
  }

  @Test
  @DisplayName("출처 목록 조회 - 정상 요청 시 200 OK + 출처 리스트 반환")
  void findSource_ReturnsSourceList_WhenRequestIsValid() throws Exception {

    // given
    // 서비스에서 반환할 출처 목록 설정
    given(newsArticleService.findAllSources())
        .willReturn(List.of(ArticleSource.NAVER, ArticleSource.YEONHAP));

    // when & then
    // GET /api/articles/sources 요청 시
    // 200 OK와 함께 JSON 배열 형태로 반환되는지 검증
    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk())

        // JSON 배열 길이 검증
        .andExpect(jsonPath("$.length()").value(2))

        // 각 요소 값 검증 (Enum → String 변환 확인)
        .andExpect(jsonPath("$[0]").value("NAVER"))
        .andExpect(jsonPath("$[1]").value("YEONHAP"));

    // then
    // 컨트롤러가 서비스 호출을 정확히 수행했는지 검증
    verify(newsArticleService, times(1)).findAllSources();
  }

  @Test
  @DisplayName("출처 목록 조회 - 서비스 예외 발생 시 500")
  void findSource_ReturnsInternalServerError_WhenServiceThrows() throws Exception {

    // given
    doThrow(new RuntimeException("출처 조회 실패"))
        .when(newsArticleService).findAllSources();

    // when & then
    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isInternalServerError());

    // 서비스는 호출됨 (예외 발생)
    verify(newsArticleService).findAllSources();
  }

  @Test
  @DisplayName("출처 목록 조회 - POST 요청 시 500 Internal Server Error")
  void findSource_ReturnsInternalServerError_WhenMethodIsInvalid() throws Exception {

    // when & then
    // /api/articles/sources는 GET만 지원한다.
    // POST 요청 시 HttpRequestMethodNotSupportedException이 발생한다.
    // 현재 GlobalExceptionHandler 정책상 해당 예외는 500으로 처리된다.
    mockMvc.perform(post("/api/articles/sources"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.exceptionType").value("HttpRequestMethodNotSupportedException"))
        .andExpect(jsonPath("$.status").value(500));

    // 매핑 단계에서 실패하므로 서비스는 호출되지 않는다.
    verify(newsArticleService, never()).findAllSources();
  }

  @Test
  @DisplayName("뉴스기사 논리삭제에 성공한다.")
  void softDelete_ReturnsNoContent_WhenArticleExists() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();

    // void 메서드는 doNothing으로 설정
    doNothing().when(newsArticleService).softDelete(articleId);

    // when & then
    mockMvc.perform(delete("/api/articles/{articleId}", articleId))
        .andExpect(status().isNoContent()); // 204 No Content

    // Service 호출 검증
    verify(newsArticleService).softDelete(articleId);
  }

  @Test
  @DisplayName("뉴스기사 논리삭제 - 서비스 예외 발생 시 500 반환")
  void softDelete_ReturnsInternalServerError_WhenServiceThrows() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();

    // 서비스에서 예외가 발생하는 상황을 가정한다.
    doThrow(new RuntimeException("논리삭제 실패"))
        .when(newsArticleService).softDelete(articleId);

    // when & then
    mockMvc.perform(delete("/api/articles/{articleId}", articleId))
        .andExpect(status().isInternalServerError());

    // 서비스 호출은 수행되었는지 검증한다.
    verify(newsArticleService, times(1)).softDelete(articleId);
  }

  @Test
  @DisplayName("뉴스기사 논리삭제 - articleId UUID 형식 오류 시 500")
  void softDelete_ReturnsInternalServerError_WhenArticleIdIsInvalid() throws Exception {

    mockMvc.perform(delete("/api/articles/{articleId}", "invalid-uuid"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.exceptionType").value("MethodArgumentTypeMismatchException"));

    verify(newsArticleService, never()).softDelete(any());
  }

  @Test
  @DisplayName("뉴스기사 물리삭제에 성공한다.")
  void hardDelete_ReturnsNoContent_WhenArticleExists() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();

    doNothing().when(newsArticleService).hardDelete(articleId);

    // when & then
    mockMvc.perform(delete("/api/articles/{articleId}/hard", articleId))
        .andExpect(status().isNoContent());

    verify(newsArticleService).hardDelete(articleId);
  }

  @Test
  @DisplayName("뉴스기사 물리삭제 - 서비스 예외 발생 시 500 반환")
  void hardDelete_ReturnsInternalServerError_WhenServiceThrows() throws Exception {

    // given
    UUID articleId = UUID.randomUUID();

    // 서비스에서 예외가 발생하는 상황을 가정한다.
    doThrow(new RuntimeException("물리삭제 실패"))
        .when(newsArticleService).hardDelete(articleId);

    // when & then
    mockMvc.perform(delete("/api/articles/{articleId}/hard", articleId))
        .andExpect(status().isInternalServerError());

    // 서비스 호출은 수행되었는지 검증한다.
    verify(newsArticleService, times(1)).hardDelete(articleId);
  }

  @Test
  @DisplayName("뉴스기사 물리삭제 - articleId UUID 형식 오류 시 500")
  void hardDelete_ReturnsInternalServerError_WhenArticleIdIsInvalid() throws Exception {

    mockMvc.perform(delete("/api/articles/{articleId}/hard", "invalid-uuid"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.exceptionType").value("MethodArgumentTypeMismatchException"));

    verify(newsArticleService, never()).hardDelete(any());
  }

  @Test
  @DisplayName("복구 API - 정상 요청 시 200 OK 반환")
  void restore_ReturnsOk_WhenRequestIsValid() throws Exception {

    // given
    List<RestoreResultDto> result = List.of();

    given(newsArticleRestoreService.restore(any(), any()))
        .willReturn(result);

    // when & then
    mockMvc.perform(
            get("/api/articles/restore")
                .param("from", "2024-01-01")
                .param("to", "2024-01-10")
        )
        .andExpect(status().isOk());

    verify(newsArticleRestoreService, times(1)).restore(any(), any());
  }

  @Test
  @DisplayName("복구 API - 날짜 형식이 잘못되면 500 반환")
  void restore_ReturnsInternalServerError_WhenDateFormatInvalid() throws Exception {

    mockMvc.perform(
            get("/api/articles/restore")
                .param("from", "invalid-date")
                .param("to", "2024-01-10")
        )
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.exceptionType").value("ResponseStatusException"))
        .andExpect(jsonPath("$.status").value(500));

    verify(newsArticleRestoreService, never()).restore(any(), any());
  }

  @Test
  @DisplayName("복구 API - 서비스 예외 발생 시 500")
  void restore_ReturnsInternalServerError_WhenServiceThrows() throws Exception {

    doThrow(new RuntimeException("복구 실패"))
        .when(newsArticleRestoreService).restore(any(), any());

    mockMvc.perform(
            get("/api/articles/restore")
                .param("from", "2024-01-01")
                .param("to", "2024-01-10")
        )
        .andExpect(status().isInternalServerError());

    verify(newsArticleRestoreService).restore(any(), any());
  }
}