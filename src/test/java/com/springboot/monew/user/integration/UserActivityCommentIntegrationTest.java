package com.springboot.monew.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.comment.service.CommentLikeService;
import com.springboot.monew.comment.service.CommentService;
import com.springboot.monew.common.integration.BaseIntegrationsTest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.user.document.UserActivityDocument;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.response.UserDto;
import com.springboot.monew.user.outbox.UserActivityOutbox;
import com.springboot.monew.user.outbox.enums.UserActivityAggregateType;
import com.springboot.monew.user.outbox.enums.UserActivityEventType;
import com.springboot.monew.user.outbox.enums.UserActivityOutboxStatus;
import com.springboot.monew.user.repository.UserActivityOutboxRepository;
import com.springboot.monew.user.repository.UserActivityRepository;
import com.springboot.monew.user.repository.UserRepository;
import com.springboot.monew.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserActivityCommentIntegrationTest extends BaseIntegrationsTest {

  @Autowired
  private UserService userService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private CommentLikeService commentLikeService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Autowired
  private UserActivityOutboxRepository userActivityOutboxRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  private UserDto writer;
  private UserDto liker;
  private NewsArticle article;

  @BeforeEach
  void setUp() {
    // 테스트 간 데이터 격리를 위해 Mongo 활동 문서와 댓글/기사 관련 RDB 데이터를 초기화한다.
    userActivityRepository.deleteAll();
    userActivityOutboxRepository.deleteAll();
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userRepository.deleteAll();

    // 댓글 작성자와 좋아요 사용자를 실제 회원가입 서비스로 생성해 사용자 활동 문서를 함께 준비한다.
    writer = userService.register(new UserRegisterRequest(
        "writer@test.com",
        "writerUser",
        "password123!"
    ));
    liker = userService.register(new UserRegisterRequest(
        "liker@test.com",
        "likerUser",
        "password123!"
    ));

    // 회원가입 시 생성된 USER_REGISTERED Outbox 이벤트는 댓글 흐름 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();

    article = newsArticleRepository.save(NewsArticle.builder()
        .source(ArticleSource.NAVER)
        .originalLink("https://example.com/article/user-activity-comment")
        .title("댓글 활동 테스트 기사")
        .publishedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .summary("댓글 활동 테스트 요약")
        .build());
  }

  @Test
  @DisplayName("댓글 작성 이후 활동 문서에 댓글 내역과 COMMENT_CREATED Outbox 이벤트가 추가된다")
  void createComment_addsCommentToUserActivityAndCreatesOutbox() {
    // given
    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(),
        writer.id(),
        "첫 댓글입니다."
    );

    // when
    // 실제 댓글 생성 서비스 진입점을 호출해 댓글 저장, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    CommentDto created = commentService.create(request);

    // then
    assertThat(created.articleId()).isEqualTo(article.getId());
    assertThat(created.userId()).isEqualTo(writer.id());
    assertThat(created.content()).isEqualTo("첫 댓글입니다.");

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.COMMENT_CREATED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.COMMENT);
    assertThat(outbox.getAggregateId()).isEqualTo(created.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains("첫 댓글입니다.");

    UserActivityDocument activity = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(activity.getComments()).hasSize(1);
    assertThat(activity.getComments().get(0).id()).isEqualTo(created.id());
    assertThat(activity.getComments().get(0).content()).isEqualTo("첫 댓글입니다.");
    assertThat(activity.getComments().get(0).articleId()).isEqualTo(article.getId());
  }

  @Test
  @DisplayName("댓글 수정 이후 활동 문서 댓글 내역과 COMMENT_UPDATED Outbox 이벤트가 함께 갱신된다")
  void updateComment_updatesCommentInUserActivityAndCreatesOutbox() {
    // given
    CommentDto created = commentService.create(new CommentRegisterRequest(
        article.getId(),
        writer.id(),
        "수정 전 댓글"
    ));
    CommentUpdateRequest request = new CommentUpdateRequest("수정 후 댓글");

    // 댓글 생성 시 발생한 Outbox 이벤트는 수정 흐름 검증 대상에서 제외하기 위해 비운다.
    userActivityOutboxRepository.deleteAll();

    // 수정 전 댓글이 사용자 활동 문서에 먼저 반영되었는지 확인한다.
    UserActivityDocument beforeUpdate = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(beforeUpdate.getComments()).hasSize(1);
    assertThat(beforeUpdate.getComments().get(0).content()).isEqualTo("수정 전 댓글");

    // when
    // 실제 댓글 수정 서비스 진입점을 호출해 댓글 수정, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    CommentDto updated = commentService.update(created.id(), writer.id(), request);

    // then
    assertThat(updated.id()).isEqualTo(created.id());
    assertThat(updated.content()).isEqualTo("수정 후 댓글");

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.COMMENT_UPDATED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.COMMENT);
    assertThat(outbox.getAggregateId()).isEqualTo(created.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);
    assertThat(outbox.getPayload()).contains("수정 후 댓글");

    UserActivityDocument activity = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(activity.getComments()).hasSize(1);
    assertThat(activity.getComments().get(0).content()).isEqualTo("수정 후 댓글");
  }

  @Test
  @DisplayName("댓글 물리 삭제 이후 활동 문서 댓글 내역이 제거되고 COMMENT_DELETED Outbox 이벤트가 저장된다")
  void hardDeleteComment_removesCommentFromUserActivityAndCreatesOutbox() {
    // given
    CommentDto created = commentService.create(new CommentRegisterRequest(
        article.getId(),
        writer.id(),
        "삭제될 댓글"
    ));
    userActivityOutboxRepository.deleteAll();

    // when
    // 실제 댓글 물리 삭제 서비스 진입점을 호출해 댓글 삭제, Outbox 저장, 활동 문서 반영을 한 흐름으로 검증한다.
    commentService.hardDelete(created.id());

    // then
    assertThat(commentRepository.findById(created.id())).isEmpty();

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(1);

    UserActivityOutbox outbox = outboxes.get(0);
    assertThat(outbox.getEventType()).isEqualTo(UserActivityEventType.COMMENT_DELETED);
    assertThat(outbox.getAggregateType()).isEqualTo(UserActivityAggregateType.COMMENT);
    assertThat(outbox.getAggregateId()).isEqualTo(created.id());
    assertThat(outbox.getStatus()).isEqualTo(UserActivityOutboxStatus.PENDING);

    UserActivityDocument activity = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(activity.getComments()).isEmpty();
  }

  @Test
  @DisplayName("댓글 좋아요 이후 사용자 활동 문서에는 좋아요 내역이 추가되고 '댓글 작성자' 활동 문서에는 좋아요 수가 반영된다")
  void likeComment_addsCommentLikeToLikerActivityAndUpdatesWriterLikeCount() {
    // given
    CommentDto created = commentService.create(new CommentRegisterRequest(
        article.getId(),
        writer.id(),
        "좋아요 대상 댓글"
    ));
    userActivityOutboxRepository.deleteAll();

    // when
    // 실제 댓글 좋아요 서비스 진입점을 호출해 좋아요 저장, Outbox 저장, 좋아요 사용자/작성자 활동 문서 반영을 함께 검증한다.
    CommentLikeDto liked = commentLikeService.like(created.id(), liker.id());

    // then
    assertThat(liked.commentId()).isEqualTo(created.id());
    assertThat(liked.likeBy()).isEqualTo(liker.id());
    assertThat(liked.commentLikeCount()).isEqualTo(1L);

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(2);
    assertThat(outboxes).extracting(UserActivityOutbox::getEventType)
        .containsExactlyInAnyOrder(
            UserActivityEventType.COMMENT_LIKED,
            UserActivityEventType.COMMENT_LIKE_COUNT_UPDATED
        );

    UserActivityDocument likerActivity = userActivityRepository.findById(liker.id()).orElseThrow();
    assertThat(likerActivity.getCommentLikes()).hasSize(1);
    assertThat(likerActivity.getCommentLikes().get(0).commentId()).isEqualTo(created.id());
    assertThat(likerActivity.getCommentLikes().get(0).commentLikeCount()).isEqualTo(1L);

    UserActivityDocument writerActivity = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(writerActivity.getComments()).hasSize(1);
    assertThat(writerActivity.getComments().get(0).id()).isEqualTo(created.id());
    assertThat(writerActivity.getComments().get(0).likeCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 이후 사용자 활동 문서에서는 좋아요 내역이 제거되고 '댓글 작성자' 활동 문서에는 좋아요 수 감소가 반영된다")
  void unlikeComment_removesCommentLikeFromLikerActivityAndDecreasesWriterLikeCount() {
    // given
    CommentDto created = commentService.create(new CommentRegisterRequest(
        article.getId(),
        writer.id(),
        "좋아요 취소 대상 댓글"
    ));
    commentLikeService.like(created.id(), liker.id());
    userActivityOutboxRepository.deleteAll();

    // when
    // 실제 댓글 좋아요 취소 서비스 진입점을 호출해 좋아요 삭제, Outbox 저장, 좋아요 사용자/작성자 활동 문서 반영을 함께 검증한다.
    commentLikeService.unlike(created.id(), liker.id());

    // then
    assertThat(commentLikeRepository.findAll()).isEmpty();

    List<UserActivityOutbox> outboxes = userActivityOutboxRepository.findAll();
    assertThat(outboxes).hasSize(2);
    assertThat(outboxes).extracting(UserActivityOutbox::getEventType)
        .containsExactlyInAnyOrder(
            UserActivityEventType.COMMENT_UNLIKED,
            UserActivityEventType.COMMENT_LIKE_COUNT_UPDATED
        );

    UserActivityDocument likerActivity = userActivityRepository.findById(liker.id()).orElseThrow();
    assertThat(likerActivity.getCommentLikes()).isEmpty();

    UserActivityDocument writerActivity = userActivityRepository.findById(writer.id()).orElseThrow();
    assertThat(writerActivity.getComments()).hasSize(1);
    assertThat(writerActivity.getComments().get(0).id()).isEqualTo(created.id());
    assertThat(writerActivity.getComments().get(0).likeCount()).isZero();
  }
}
