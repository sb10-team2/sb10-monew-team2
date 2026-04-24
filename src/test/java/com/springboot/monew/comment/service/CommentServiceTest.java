package com.springboot.monew.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentPageRequest;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.dto.CursorPageResponseCommentDto;
import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.exception.CommentException;
import com.springboot.monew.comment.mapper.CommentMapper;
import com.springboot.monew.comment.repository.CommentLikeRepository;
import com.springboot.monew.comment.repository.CommentRepository;
import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.common.exception.MonewException;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.exception.NewsArticleErrorCode;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock CommentRepository commentRepository;
  @Mock CommentLikeRepository commentLikeRepository;
  @Mock NewsArticleRepository articleRepository;
  @Mock UserRepository userRepository;
  @Mock CommentMapper commentMapper;

  @InjectMocks
  private CommentService commentService;

  @Test
  @DisplayName("댓글 등록에 성공한다")
  void create_성공() {
    // given
    User user = new User("email", "nickname", "password");
    NewsArticle article = new NewsArticle(
        ArticleSource.NAVER, "https://test.com", "제목", Instant.now(), "요약");

    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(),
        user.getId(),
        "테스트 댓글입니다."
    );
    CommentDto expected = new CommentDto(
        UUID.randomUUID(),
        article.getId(),
        user.getId(),
        "nickname",
        "테스트 댓글입니다.",
        0L,
        false,
        Instant.now()
    );

    given(articleRepository.findById(article.getId())).willReturn(Optional.of(article));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentMapper.toCommentDto(any(Comment.class), eq(false))).willReturn(expected);

    // when
    CommentDto result = commentService.create(request);

    // then
    assertThat(result).isEqualTo(expected);
    assertThat(result.content()).isEqualTo(request.content());
    assertThat(result.likeByMe()).isEqualTo(false);
    assertThat(result.likeCount()).isEqualTo(0L);

    verify(articleRepository).findById(article.getId());
    verify(userRepository).findById(user.getId());
    verify(commentMapper).toCommentDto(any(Comment.class), eq(false));

  }

  @Test
  @DisplayName("Article이 존재하지 않는 경우는 ArticleNotFound를 반환한다")
  void create_실패_ArticleNotFound() {
    // given
    UUID articleId = UUID.randomUUID();
    CommentRegisterRequest request = new CommentRegisterRequest(articleId, UUID.randomUUID(), "테스트 댓글");

    given(articleRepository.findById(articleId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.create(request))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository, never()).findById(any());
    verify(commentMapper, never()).toCommentDto(any(), eq(false));
    verify(commentRepository, never()).save(any());
  }

  @Test
  @DisplayName("User가 존재하지 않는 경우는 UserNotFound를 반환한다")
  void create_실패_UserNotFound() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    NewsArticle article = mock(NewsArticle.class);
    CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "테스트 댓글");

    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.create(request))
        .isInstanceOf(MonewException.class)
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository).findById(userId);
    verify(commentMapper, never()).toCommentDto(any(), eq(false));
    verify(commentRepository, never()).save(any());
  }

  @Test
  @DisplayName("댓글 수정에 성공한다.")
  void update_성공() throws NoSuchFieldException, IllegalAccessException {
    // given
    NewsArticle article = mock(NewsArticle.class);
    User user = new User("email", "nickname", "password");

    Comment comment = new Comment(user, article, "수정 전 댓글입니다.");

    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // id 필드 리플렉션 -> id가 DB insert 되어야 생성되서 리플렉션 사용했습니다.
    Field idField = BaseEntity.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(user, userId);
    idField.set(comment, commentId);

    CommentUpdateRequest request = new CommentUpdateRequest(
        "수정 후 댓글입니다."
    );

    // 예상 기댓값
    CommentDto expected = new CommentDto(
        UUID.randomUUID(),
        UUID.randomUUID(),
        user.getId(),
        "nickname",
        "수정 후 댓글입니다.",
        0L,
        false,
        Instant.now()
    );


    given(commentRepository.findByIdAndIsDeletedFalse(comment.getId())).willReturn(Optional.of(comment));
    given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    given(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId())).willReturn(false);
    given(commentMapper.toCommentDto(any(Comment.class), eq(false))).willReturn(expected);

    // when
    CommentDto result = commentService.update(comment.getId(), user.getId(), request);

    // then
    assertThat(result).isEqualTo(expected);
    assertThat(result.content()).isEqualTo(expected.content());
    assertThat(result.likeByMe()).isEqualTo(expected.likeByMe());
    assertThat(result.likeCount()).isEqualTo(expected.likeCount());

    verify(commentRepository).findByIdAndIsDeletedFalse(comment.getId());
    verify(userRepository).findById(user.getId());
    verify(commentMapper).toCommentDto(any(Comment.class), eq(false));
  }

  @Test
  @DisplayName("comment가 존재하지 않는 경우 댓글 수정에 실패한다.")
  void update_실패_CommentNotFound() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정할 댓글");

    given(commentRepository.findByIdAndIsDeletedFalse(commentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.update(commentId, userId, request))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", commentId));
        });

    verify(commentRepository).findByIdAndIsDeletedFalse(commentId);
    verify(userRepository, never()).findById(any());
    verify(commentMapper, never()).toCommentDto(any(), eq(false));
  }

  @Test
  @DisplayName("user가 존재하지 않는 경우 댓글 수정에 실패한다.")
  void update_실패_UserNotFound() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정할 댓글");
    Comment comment = mock(Comment.class);

    given(commentRepository.findByIdAndIsDeletedFalse(commentId)).willReturn(Optional.of(comment));
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.update(commentId, userId, request))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(commentRepository).findByIdAndIsDeletedFalse(commentId);
    verify(userRepository).findById(userId);
    verify(commentMapper, never()).toCommentDto(any(), eq(false));
  }

  @Test
  @DisplayName("본인 댓글이 아닌 경우 수정에 실패한다.")
  void update_실패_CommentNotOwnedByUser() throws NoSuchFieldException, IllegalAccessException {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID wrongUserId = UUID.randomUUID();

    NewsArticle article = mock(NewsArticle.class);
    User user = new User("email", "nickname", "password");
    Comment comment = new Comment(user, article, "댓글이다.");

    CommentUpdateRequest request = new CommentUpdateRequest("수정했다.");

    // 리플렉션 get으로 id에 접근해야 함.
    Field idField = BaseEntity.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(user, wrongUserId);
    idField.set(comment, commentId);


    given(commentRepository.findByIdAndIsDeletedFalse(commentId)).willReturn(Optional.of(comment));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when
    assertThatThrownBy(() -> commentService.update(commentId, userId, request))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_OWNED_BY_USER);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", commentId));
        });

    // then
    verify(commentRepository).findByIdAndIsDeletedFalse(commentId);
    verify(userRepository).findById(userId);
    verify(commentMapper, never()).toCommentDto(any(), eq(false));
  }

  @Test
  @DisplayName("논리 삭제가 되었을 경우에는 댓글의 isDeleted 필드가 true가 되어야 한다")
  void softDelete_성공() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = mock(Comment.class);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    given(comment.isDeleted()).willReturn(false);

    // when
    commentService.softDelete(commentId);

    // then
    verify(commentRepository).findById(commentId);
    verify(comment).delete();
  }

  @Test
  @DisplayName("존재하지 않는 댓글은 논리 삭제에 실패한다.")
  void softDelete_실패_COMMENT_NOT_FOUND() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = mock(Comment.class);

    given(commentRepository.findById(commentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.softDelete(commentId))
        .satisfies(throwable -> {
          CommentException exception = (CommentException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", commentId));
        });

    verify(commentRepository).findById(commentId);
    verify(comment, never()).delete();
  }

  @Test
  @DisplayName("이미 논리 삭제된 댓글은 논리 삭제에 실패한다.")
  void softDelete_실패_COMMENT_ALREADY_DELETED() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = mock(Comment.class);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    given(comment.isDeleted()).willReturn(true);

    // when
    assertThatThrownBy(() -> commentService.softDelete(commentId))
        .satisfies(throwable -> {
          CommentException exception = (CommentException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_ALREADY_DELETED);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", commentId));
        });

    verify(commentRepository).findById(commentId);
    verify(comment, never()).delete();
  }

  @Test
  @DisplayName("댓글 물리 삭제에 성공한다.")
  void hardDelete_성공() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = mock(Comment.class);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    // when
    commentService.hardDelete(commentId);

    // then
    verify(commentRepository).findById(commentId);
    verify(commentRepository).delete(comment);
  }

  @Test
  @DisplayName("존재하지 않는 댓글일 경우 댓글 물리 삭제에 실패한다.")
  void hardDelete_실패_CommentNotFound() {
    // given
    UUID commentId = UUID.randomUUID();
    Comment comment = mock(Comment.class);

    given(commentRepository.findById(commentId)).willReturn(Optional.empty());
    // when
    assertThatThrownBy(() -> commentService.hardDelete(commentId))
        .satisfies(throwable -> {
          CommentException exception = (CommentException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("commentId", commentId));
        });
    verify(commentRepository).findById(commentId);
    verify(commentRepository, never()).delete(comment);
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - hasNext = false")
  void list_성공_hasNext_false() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // Article, User는 실제 동작이 필요 없고 isDeleted() 반환값만 제어하면 되므로 mock 사용
    NewsArticle article = mock(NewsArticle.class);
    User user = mock(User.class);

    // 기사 존재하고 삭제되지 않은 상태
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(false);

    // 유저 존재하고 삭제되지 않은 상태
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);

    UUID commentId1 = UUID.randomUUID();
    UUID commentId2 = UUID.randomUUID();

    // Comment도 getId() 제어가 필요하므로 mock 사용
    // (getId()는 JPA persist 시점에 세팅되므로 new로 만들면 null)
    Comment comment1 = mock(Comment.class);
    Comment comment2 = mock(Comment.class);

    given(comment1.getId()).willReturn(commentId1);
    given(comment2.getId()).willReturn(commentId2);

    // mapper가 반환할 실제 CommentDto 객체 (mock 사용 시 필드가 null이라 id 검증 불가)
    CommentDto commentDto1 = new CommentDto(commentId1, articleId, userId, "nickname", "댓글1", 0L, true, Instant.now());
    CommentDto commentDto2 = new CommentDto(commentId2, articleId, userId, "nickname", "댓글2", 0L, true, Instant.now());

    CommentPageRequest request = new CommentPageRequest(
        articleId,
        CommentOrderBy.createdAt,
        CommentDirection.DESC,
        null,
        null,
        10
    );

    // 서비스 내부에서 limit + 1로 조회함 (다음 페이지 존재 여부 판단용)
    // 반환된 댓글 수(2)가 limit(10)보다 작으므로 hasNext = false
    given(commentRepository.findComments(
        request.articleId(),
        request.orderBy(),
        request.direction(),
        request.cursor(),
        request.after(),
        request.limit() + 1
    )).willReturn(List.of(comment1, comment2));

    // 두 댓글 모두 userId가 좋아요 누른 상태
    given(commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(userId, List.of(commentId1, commentId2)))
        .willReturn(List.of(commentId1, commentId2));

    // 해당 기사의 삭제되지 않은 전체 댓글 수
    given(commentRepository.countByArticleIdAndIsDeletedFalse(articleId)).willReturn(2L);

    // comment1, comment2 모두 좋아요 누른 상태이므로 likeByMe = true
    given(commentMapper.toCommentDto(comment1, true)).willReturn(commentDto1);
    given(commentMapper.toCommentDto(comment2, true)).willReturn(commentDto2);

    // when
    CursorPageResponseCommentDto<CommentDto> result = commentService.list(request, userId);

    // then
    assertThat(result.hasNext()).isFalse();                                    // 2개 < limit(10)이므로 다음 페이지 없음
    assertThat(result.content()).hasSize(2);                                   // 댓글 2개 반환
    assertThat(result.content().get(0).id()).isEqualTo(commentId1);            // 첫 번째 댓글 id 확인
    assertThat(result.content().get(1).id()).isEqualTo(commentId2);            // 두 번째 댓글 id 확인
    assertThat(result.content().get(0).likeByMe()).isTrue();                   // 좋아요 누른 댓글은 likeByMe = true
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextAfter()).isNull();
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - hasNext = true")
  void list_성공_hasNext_true() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // 페이지네이션 요청 Dto
    CommentPageRequest request = new CommentPageRequest(
        articleId,
        CommentOrderBy.likeCount,
        CommentDirection.DESC,
        null,
        null,
        2
    );

    NewsArticle article = mock(NewsArticle.class);
    User user = mock(User.class);

    // 기사, 사용자 삭제되지 않은 상태
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(false);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(false);


    // limit=2, 3개 반환(limit+1)
    Comment comment1 = mock(Comment.class);
    Comment comment2 = mock(Comment.class);
    Comment comment3 = mock(Comment.class);

    UUID commentId1 = UUID.randomUUID();
    UUID commentId2 = UUID.randomUUID();
    UUID commentId3 = UUID.randomUUID();

    given(comment1.getId()).willReturn(commentId1);
    given(comment2.getId()).willReturn(commentId2);
    given(comment3.getId()).willReturn(commentId3);
    given(comment1.getLikeCount()).willReturn(5L);

    // nextAfter는 subList 후 마지막 댓글(comment1)의 createdAt
    Instant comment1CreatedAt = Instant.now().minusSeconds(5);
    given(comment1.getCreatedAt()).willReturn(comment1CreatedAt);

    // mapper가 반환할 실제 CommentDto 객체 (mock 사용 시 필드가 null이라 id 검증 불가)
    CommentDto commentDto1 = new CommentDto(commentId1, articleId, userId, "nickname", "댓글1", 5L, true, Instant.now());
    CommentDto commentDto2 = new CommentDto(commentId2, articleId, userId, "nickname", "댓글2", 10L, false, Instant.now());


    // 좋아요 순 페이지네이션 시 comment2, comment1 순서로
    given(commentRepository.findComments(
        request.articleId(),
        request.orderBy(),
        request.direction(),
        request.cursor(),
        request.after(),
        request.limit() + 1))
        .willReturn(List.of(comment2, comment1, comment3));

    // comment1만 좋아요 누른 걸 가정
    given(commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(userId, List.of(commentId2, commentId1, commentId3)))
    .willReturn(List.of(commentId1));

    // 댓글 전체 개수 3개
    given(commentRepository.countByArticleIdAndIsDeletedFalse(articleId))
        .willReturn(3L);

    given(commentMapper.toCommentDto(comment1, true)).willReturn(commentDto1);
    given(commentMapper.toCommentDto(comment2, false)).willReturn(commentDto2);

    // when
    CursorPageResponseCommentDto<CommentDto> result = commentService.list(request, userId);

    // then
    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).id()).isEqualTo(commentId2);
    assertThat(result.content().get(1).id()).isEqualTo(commentId1);
    assertThat(result.content().get(0).likeByMe()).isFalse();
    assertThat(result.content().get(1).likeByMe()).isTrue();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo("5");
    assertThat(result.nextAfter()).isEqualTo(comment1CreatedAt);
    assertThat(result.totalElements()).isEqualTo(3L);
  }

  @Test
  @DisplayName("존재하지 않는 기사로 댓글 목록 조회 시 실패한다")
  void list_실패_ArticleNotFound() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentPageRequest request = new CommentPageRequest(
        articleId, CommentOrderBy.createdAt, CommentDirection.DESC, null, null, 10);

    given(articleRepository.findById(articleId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.list(request, userId))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository, never()).findById(any());
    verify(commentRepository, never()).findComments(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  @DisplayName("논리 삭제된 기사로 댓글 목록 조회 시 실패한다")
  void list_실패_ArticleAlreadyDeleted() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentPageRequest request = new CommentPageRequest(
        articleId, CommentOrderBy.createdAt, CommentDirection.DESC, null, null, 10);

    NewsArticle article = mock(NewsArticle.class);
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(true);

    // when & then
    assertThatThrownBy(() -> commentService.list(request, userId))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(NewsArticleErrorCode.NEWS_ARTICLE_ALREADY_DELETED);
          assertThat(exception.getDetails()).isEqualTo(Map.of("articleId", articleId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository, never()).findById(any());
    verify(commentRepository, never()).findComments(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  @DisplayName("존재하지 않는 유저로 댓글 목록 조회 시 실패한다")
  void list_실패_UserNotFound() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentPageRequest request = new CommentPageRequest(
        articleId, CommentOrderBy.createdAt, CommentDirection.DESC, null, null, 10);

    NewsArticle article = mock(NewsArticle.class);
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(false);
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.list(request, userId))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository).findById(userId);
    verify(commentRepository, never()).findComments(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  @DisplayName("논리 삭제된 유저로 댓글 목록 조회 시 실패한다")
  void list_실패_UserAlreadyDeleted() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentPageRequest request = new CommentPageRequest(
        articleId, CommentOrderBy.createdAt, CommentDirection.DESC, null, null, 10);

    NewsArticle article = mock(NewsArticle.class);
    User user = mock(User.class);
    given(articleRepository.findById(articleId)).willReturn(Optional.of(article));
    given(article.isDeleted()).willReturn(false);
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(user.isDeleted()).willReturn(true);

    // when & then
    assertThatThrownBy(() -> commentService.list(request, userId))
        .satisfies(throwable -> {
          MonewException exception = (MonewException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(articleRepository).findById(articleId);
    verify(userRepository).findById(userId);
    verify(commentRepository, never()).findComments(any(), any(), any(), any(), any(), anyInt());
  }
}