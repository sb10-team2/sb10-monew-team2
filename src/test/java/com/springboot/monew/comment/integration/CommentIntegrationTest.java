package com.springboot.monew.comment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.newsarticle.enums.ArticleSource;
import com.springboot.monew.newsarticle.repository.NewsArticleRepository;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class CommentIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  private User user;
  private NewsArticle article;

  @BeforeEach
  void setUp() {
    commentRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userActivityRepository.deleteAll();
    userRepository.deleteAll();

    // 테스트용 유저 및 뉴스기사 생성
    user = userRepository.save(new User("test@test.com", "tester", "password1234!"));
    // 사용자 활동내역 문서도 미리 생성
    userActivityRepository.save(
        new UserActivityDocument(user.getId(), user.getEmail(), user.getNickname(), Instant.now()));
    article = newsArticleRepository.save(NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("https://test.com/article/1")
        .title("테스트 뉴스")
        .publishedAt(Instant.now())
        .summary("테스트 요약")
        .build());
  }

  @Test
  @DisplayName("댓글 등록 시 201 상태코드와 생성된 댓글 정보를 반환한다")
  void createComment_Returns201_WhenValidRequest() {
    // given
    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(), user.getId(), "테스트 댓글입니다.");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CommentRegisterRequest> entity = new HttpEntity<>(request, headers);

    // when, restTemplate으로 실제 요청을 보냄
    ResponseEntity<CommentDto> response = restTemplate.postForEntity(
        "/api/comments", entity, CommentDto.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().content()).isEqualTo("테스트 댓글입니다.");
    assertThat(response.getBody().articleId()).isEqualTo(article.getId());
    assertThat(response.getBody().userId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("댓글 수정 시 200 상태코드와 수정된 댓글 정보를 반환한다")
  void updateComment_Returns200_WhenValidRequest() {
    // given
    // 수정 대상 댓글 사전 생성
    Comment comment = commentRepository.save(new Comment(user, article, "원본 댓글"));
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글입니다.");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Monew-Request-User-ID", user.getId().toString());
    HttpEntity<CommentUpdateRequest> entity = new HttpEntity<>(request, headers);

    // when
    ResponseEntity<CommentDto> response = restTemplate.exchange(
        "/api/comments/" + comment.getId(),
        HttpMethod.PATCH, entity, CommentDto.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().userId()).isEqualTo(user.getId());
    assertThat(response.getBody().articleId()).isEqualTo(article.getId());
    assertThat(response.getBody().content()).isEqualTo("수정된 댓글입니다.");
  }

  @Test
  @DisplayName("댓글 논리 삭제 시 204 상태코드를 반환하고 isDeleted가 true가 된다")
  void softDeleteComment_Returns204_WhenValidRequest() {
    // given
    Comment comment = commentRepository.save(new Comment(user, article, "삭제할 댓글"));

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/comments/" + comment.getId(),
        HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // DB에서 실제로 논리 삭제되었는지 검증
    Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(deleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("존재하지 않는 댓글 수정 시 404 상태코드를 반환한다")
  void updateComment_Returns404_WhenCommentNotFound() {
    // given
    UUID nonExistId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Monew-Request-User-ID", user.getId().toString());
    HttpEntity<CommentUpdateRequest> entity = new HttpEntity<>(request, headers);

    // when
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/comments/" + nonExistId,
        HttpMethod.PATCH, entity, ErrorResponse.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("CM01");
    assertThat(response.getBody().getMessage()).isEqualTo("댓글을 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("댓글 등록 시 사용자 활동 내역에 댓글이 추가된다")
  void createComment_AddsCommentToUserActivity_WhenValidRequest() {
    // given
    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(), user.getId(), "활동 내역 테스트 댓글");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CommentRegisterRequest> entity = new HttpEntity<>(request, headers);

    // when
    // 실제 HTTP 요청으로 댓글 생성 → 서비스 내부에서 CommentCreatedEvent 발행
    restTemplate.postForEntity("/api/comments", entity, CommentDto.class);

    // then
    // @TransactionalEventListener(AFTER_COMMIT)으로 처리되므로 트랜잭션 커밋 이후 MongoDB에 반영 ?
    UserActivityDocument activity = userActivityRepository.findById(user.getId()).orElseThrow();
    assertThat(activity.getComments()).hasSize(1);
    assertThat(activity.getComments().get(0).content()).isEqualTo("활동 내역 테스트 댓글");
    assertThat(activity.getComments().get(0).articleId()).isEqualTo(article.getId());
  }

  @Test
  @DisplayName("댓글 물리 삭제 시 204 상태코드를 반환하고 DB에서 완전히 삭제된다")
  void hardDeleteComment_Returns204_WhenValidRequest() {
    // given
    Comment comment = commentRepository.save(new Comment(user, article, "물리 삭제할 댓글"));

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/hard",
        HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // DB에서 사라졌는지 검증
    assertThat(commentRepository.findById(comment.getId())).isEmpty();
  }
}
