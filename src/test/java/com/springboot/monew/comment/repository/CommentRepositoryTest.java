package com.springboot.monew.comment.repository;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentDirection;
import com.springboot.monew.comment.entity.CommentOrderBy;
import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentRepositoryTest extends BaseRepositoryTest {

  @Autowired
  CommentRepository commentRepository;

  private NewsArticle article;

  @BeforeEach
  void setUp() {
    queryInspector.clear();
    article = testEntityManager.generateNewsArticle();
  }

  @Test
  @DisplayName("삭제되지 않은 댓글 조회 성공 - user JOIN FETCH로 쿼리 1번")
  void findByIdAndIsDeletedFalse_ReturnsComment_WhenCommentExists() {
    // given
    User user = testEntityManager.generateUser();
    Comment comment = new Comment(user, article, "테스트 댓글");
    commentRepository.save(comment);
    flushAndClear();
    queryInspector.clear();

    // when
    Comment result = commentRepository.findByIdAndIsDeletedFalse(comment.getId()).orElseThrow();

    // then
    Assertions.assertThat(result.getId()).isEqualTo(comment.getId());
    Assertions.assertThat(result.isDeleted()).isFalse();
    // JOIN FETCH로 user까지 한 번에 가져왔는지 확인
    Assertions.assertThat(result.getUser().getId()).isEqualTo(user.getId());
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("논리 삭제된 댓글은 조회되지 않는다")
  void findByIdAndIsDeletedFalse_ReturnsEmpty_WhenCommentDeleted() {
    // given
    User user = testEntityManager.generateUser();
    Comment comment = new Comment(user, article, "테스트 댓글");
    commentRepository.save(comment);
    comment.delete();
    flushAndClear();
    queryInspector.clear();

    // when
    var result = commentRepository.findByIdAndIsDeletedFalse(comment.getId());

    // then
    Assertions.assertThat(result).isEmpty();
    ensureQueryCount(1);
  }

  @Test
  @DisplayName("좋아요 수 1 증가 성공")
  void incrementLikeCount_IncreasesLikeCountByOne_WhenCommentExists() {
    // given
    NewsArticle article = testEntityManager.generateNewsArticle();
    User user = testEntityManager.generateUser();
    Comment comment = new Comment(user, article, "테스트 댓글");
    commentRepository.save(comment);
    flushAndClear();
    queryInspector.clear();

    // when
    commentRepository.incrementLikeCount(comment.getId());
    flushAndClear();

    // then
    Comment result = commentRepository.findById(comment.getId()).orElseThrow();
    Assertions.assertThat(result.getLikeCount()).isEqualTo(1);
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("좋아요 수 1 감소 성공")
  void decrementLikeCount_DecreasesLikeCountByOne_WhenLikeCountIsPositive() {
    // given
    NewsArticle article = testEntityManager.generateNewsArticle();
    User user = testEntityManager.generateUser();
    Comment comment = new Comment(user, article, "테스트 댓글");

    commentRepository.save(comment);
    commentRepository.incrementLikeCount(comment.getId());

    flushAndClear();
    queryInspector.clear();

    // when
    commentRepository.decrementLikeCount(comment.getId());
    flushAndClear();

    // then
    Comment result = commentRepository.findById(comment.getId()).orElseThrow();
    Assertions.assertThat(result.getLikeCount()).isEqualTo(0);
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("좋아요 수가 0일 때 감소 시도 → 0 유지")
  void decrementLikeCount_MaintainsZero_WhenLikeCountIsZero() {
    // given
    NewsArticle article = testEntityManager.generateNewsArticle();
    User user = testEntityManager.generateUser();
    Comment comment = new Comment(user, article, "테스트 댓글");

    commentRepository.save(comment);

    flushAndClear();
    queryInspector.clear();

    // when
    commentRepository.decrementLikeCount(comment.getId());
    flushAndClear();

    // then
    Comment result = commentRepository.findById(comment.getId()).orElseThrow();
    Assertions.assertThat(comment.getLikeCount()).isEqualTo(0);
    ensureQueryCount(2);
  }

  @Test
  @DisplayName("cursor null이면 첫 페이지 반환")
  void findComments_ReturnsFirstPage_WhenCursorIsNull() {
    // given
    testEntityManager.generateComments(3, article);
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, 10);

    // then
    Assertions.assertThat(result).hasSize(3);
  }

  @Test
  @DisplayName("createdAt DESC 정렬 - 최신순으로 반환")
  void findComments_ReturnsDescOrder_WhenOrderByCreatedAtDesc() {
    // given
    List<Comment> comments = testEntityManager.generateComments(3, article);
    Instant base = Instant.now();
    setCreatedAt(comments.get(0).getId(), base.minusSeconds(20));
    setCreatedAt(comments.get(1).getId(), base.minusSeconds(10));
    setCreatedAt(comments.get(2).getId(), base);
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, 10);

    // then
    Assertions.assertThat(result)
        .extracting(Comment::getCreatedAt)
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  @DisplayName("createdAt ASC 정렬 - 오래된순으로 반환")
  void findComments_ReturnsAscOrder_WhenOrderByCreatedAtAsc() {
    // given
    List<Comment> comments = testEntityManager.generateComments(3, article);
    Instant base = Instant.now();
    setCreatedAt(comments.get(0).getId(), base.minusSeconds(20));
    setCreatedAt(comments.get(1).getId(), base.minusSeconds(10));
    setCreatedAt(comments.get(2).getId(), base);
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.ASC,
        null, null, 10);

    // then
    Assertions.assertThat(result)
        .extracting(Comment::getCreatedAt)
        .isSortedAccordingTo(Comparator.naturalOrder());
  }

  @Test
  @DisplayName("likeCount DESC 정렬 - 좋아요 많은순으로 반환")
  void findComments_ReturnsDescOrder_WhenOrderByLikeCountDesc() {
    // given
    List<Comment> comments = testEntityManager.generateComments(3, article);
    commentRepository.incrementLikeCount(comments.get(0).getId());
    commentRepository.incrementLikeCount(comments.get(1).getId());
    commentRepository.incrementLikeCount(comments.get(1).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.likeCount, CommentDirection.DESC,
        null, null, 10);

    // then
    Assertions.assertThat(result)
        .extracting(Comment::getLikeCount)
        .isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  @DisplayName("likeCount ASC 정렬 - 좋아요 적은순으로 반환")
  void findComments_ReturnsAscOrder_WhenOrderByLikeCountAsc() {
    // given
    List<Comment> comments = testEntityManager.generateComments(3, article);
    commentRepository.incrementLikeCount(comments.get(0).getId());
    commentRepository.incrementLikeCount(comments.get(1).getId());
    commentRepository.incrementLikeCount(comments.get(1).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    commentRepository.incrementLikeCount(comments.get(2).getId());
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.likeCount, CommentDirection.ASC,
        null, null, 10);

    // then
    Assertions.assertThat(result)
        .extracting(Comment::getLikeCount)
        .isSortedAccordingTo(Comparator.naturalOrder());
  }

  @Test
  @DisplayName("cursor 있을 때 해당 커서 이후 데이터만 반환 - 1·2페이지 중복 없음")
  void findComments_ReturnsNextPageOnly_WhenCursorProvided() {
    // given
    List<Comment> comments = testEntityManager.generateComments(5, article);
    Instant base = Instant.now();
    for (int i = 0; i < comments.size(); i++) {
      setCreatedAt(comments.get(i).getId(), base.minusSeconds((long) (comments.size() - i) * 10));
    }
    flushAndClear();

    List<Comment> firstPage = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, 3);
    Comment last = firstPage.get(firstPage.size() - 1);

    // when
    List<Comment> secondPage = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        CommentOrderBy.createdAt.getCursor(last), last.getCreatedAt(), 3);

    // then
    List<UUID> firstPageIds = firstPage.stream().map(Comment::getId).toList();
    Assertions.assertThat(secondPage)
        .extracting(Comment::getId)
        .doesNotContainAnyElementsOf(firstPageIds);
  }

  @Test
  @DisplayName("논리 삭제된 댓글은 목록에서 제외")
  void findComments_ExcludesDeletedComments_WhenCommentDeleted() {
    // given
    List<Comment> comments = testEntityManager.generateComments(3, article);
    comments.get(0).delete();
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, 10);

    // then
    Assertions.assertThat(result).hasSize(2);
    Assertions.assertThat(result)
        .extracting(Comment::isDeleted)
        .containsOnly(false);
  }

  @Test
  @DisplayName("limit 적용 - 지정한 개수만큼만 반환")
  void findComments_ReturnsLimitedCount_WhenLimitProvided() {
    // given
    int limit = 3;
    testEntityManager.generateComments(5, article);
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, limit);

    // then
    Assertions.assertThat(result).hasSize(limit);
  }

  @Test
  @DisplayName("다른 article의 댓글은 제외")
  void findComments_ReturnsEmpty_WhenDifferentArticle() {
    // given
    NewsArticle otherArticle = testEntityManager.generateNewsArticle();
    testEntityManager.generateComments(3, otherArticle);
    flushAndClear();

    // when
    List<Comment> result = commentRepository.findComments(
        article.getId(), CommentOrderBy.createdAt, CommentDirection.DESC,
        null, null, 10);

    // then
    Assertions.assertThat(result).isEmpty();
  }

  private void setCreatedAt(UUID commentId, Instant createdAt) {
    em.createNativeQuery("UPDATE comments SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", commentId)
        .executeUpdate();
  }
}
