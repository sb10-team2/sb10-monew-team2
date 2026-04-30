package com.springboot.monew.newsarticles.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.s3.NewsArticleRestoreService;
import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import com.springboot.monew.newsarticles.service.NewsArticleService;
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
  @DisplayName("뉴스기사 수집 API 호출에 성공한다.")
  void collectNews_ReturnsOk_WhenRequestIsValid() throws Exception {

    // given
    // collectAll()은 반환값이 없으므로 아무 동작도 하지 않도록 설정
    doNothing().when(newsArticleCollectService).collectAll();

    // when & then
    mockMvc.perform(post("/api/articles"))
        .andExpect(status().isOk()) // HTTP 200 응답 검증
        .andExpect(content().string("뉴스 수집 완료")); // 응답 메시지 검증

    // Controller가 Service를 정상 호출했는지 검증
    verify(newsArticleCollectService).collectAll();
  }

  @Test
  @DisplayName("출처 목록 조회에 성공한다.")
  void findSource_ReturnsSourceList_WhenRequestIsValid() throws Exception {

    // given
    // Service에서 반환할 mock 데이터 설정
    given(newsArticleService.findAllSources())
        .willReturn(List.of(ArticleSource.NAVER, ArticleSource.YEONHAP));

    // when & then
    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk()); // HTTP 200 검증

    // 실제 Service 호출 여부 검증
    verify(newsArticleService).findAllSources();
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
}