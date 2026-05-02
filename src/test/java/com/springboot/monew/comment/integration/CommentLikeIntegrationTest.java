package com.springboot.monew.comment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.notification.repository.NotificationRepository;
import com.springboot.monew.user.entity.User;
import com.springboot.monew.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CommentLikeIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  private User user;
  private Comment comment;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userRepository.deleteAll();

    // 테스트용 유저, 뉴스기사, 댓글 생성
    user = userRepository.save(new User("test@test.com", "tester", "password1234!"));
    NewsArticle article = newsArticleRepository.save(NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("https://test.com/article/1")
        .title("테스트 뉴스")
        .publishedAt(Instant.now())
        .summary("테스트 요약")
        .build());
    comment = commentRepository.save(new Comment(user, article, "좋아요 테스트 댓글"));
  }

  @Test
  @DisplayName("댓글 좋아요 시 201 상태코드와 좋아요 정보를 반환한다")
  void likeComment_Returns201_WhenValidRequest() {
    // given
    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    // when
    ResponseEntity<CommentLikeDto> response = restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/comment-likes",
        HttpMethod.POST, entity, CommentLikeDto.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().commentId()).isEqualTo(comment.getId());
    assertThat(response.getBody().likeBy()).isEqualTo(user.getId());
    assertThat(response.getBody().commentLikeCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 시 200 상태코드를 반환하고 좋아요가 삭제된다")
  void unlikeComment_Returns200_WhenLikeExists() {
    // given
    // 사전에 좋아요 등록
    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/comment-likes",
        HttpMethod.POST, entity, CommentLikeDto.class);

    // when
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/comment-likes",
        HttpMethod.DELETE, entity, Void.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // DB에서 좋아요 레코드가 삭제되었는지 검증
    assertThat(commentLikeRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("이미 좋아요한 댓글에 다시 좋아요 시 409 상태코드를 반환한다")
  void likeComment_Returns409_WhenAlreadyLiked() {
    // given
    // 첫 번째 좋아요
    HttpHeaders headers = new HttpHeaders();
    headers.set("Monew-Request-User-ID", user.getId().toString());
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/comment-likes",
        HttpMethod.POST, entity, CommentLikeDto.class);

    // when
    // 두 번째 좋아요 시도
    ResponseEntity<Void> response = restTemplate.exchange(
        "/api/comments/" + comment.getId() + "/comment-likes",
        HttpMethod.POST, entity, Void.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }
}
