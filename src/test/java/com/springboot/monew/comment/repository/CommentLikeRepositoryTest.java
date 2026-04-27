package com.springboot.monew.comment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.springboot.monew.comment.entity.Comment;
import com.springboot.monew.comment.entity.CommentLike;
import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.users.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentLikeRepositoryTest extends BaseRepositoryTest {

  @Autowired
  CommentLikeRepository commentLikeRepository;

  @BeforeEach
  void setUp() {
    queryInspector.clear();
  }

  @Test
  @DisplayName("유저가 좋아요한 댓글 Id 목록 반환에 성공한다!")
  void findCommentIdsByUserIdAndCommentIdIn_성공() {
    // given
    NewsArticle article = testEntityManager.generateNewsArticle();
    List<Comment> comments = testEntityManager.generateComments(5, article);
    User user = testEntityManager.generateUser();

    // 5개 중 2개만 좋아요
    CommentLike like1 = new CommentLike(comments.get(0), user);
    CommentLike like2 = new CommentLike(comments.get(1), user);
    em.persist(like1);
    em.persist(like2);
    flushAndClear();

    List<UUID> allCommentIds = comments.stream().map(Comment::getId).toList();

    // when
    List<UUID> result = commentLikeRepository.findCommentIdsByUserIdAndCommentIdIn(user.getId(),
        allCommentIds);

    // then (size:2, Id 보유 여부 확인)
    assertEquals(2, result.size());
    assertTrue(result.contains(comments.get(0).getId()));
    assertTrue(result.contains(comments.get(1).getId()));
  }
}
