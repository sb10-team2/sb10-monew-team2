package com.springboot.monew.comment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.exception.CommentErrorCode;
import com.springboot.monew.comment.exception.CommentException;
import com.springboot.monew.comment.service.CommentLikeService;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentLikeController.class)
class CommentLikeControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  private CommentLikeService commentLikeService;

  @Test
  @DisplayName("댓글 좋아요 성공 시 201을 반환한다.")
  void like_성공() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CommentLikeDto response = new CommentLikeDto(
        UUID.randomUUID(),
        userId,
        Instant.now(),
        commentId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "작성자닉네임",
        "댓글내용",
        0,
        Instant.now()
    );

    given(commentLikeService.like(commentId, userId)).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()));

    then(commentLikeService).should().like(commentId, userId);
  }

  @Test
  @DisplayName("존재하지 않는 댓글에 좋아요 시 404를 반환한다.")
  void like_실패_댓글없음() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(commentLikeService.like(commentId, userId))
        .willThrow(new CommentException(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)));

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound());

    then(commentLikeService).should().like(commentId, userId);
  }

  @Test
  @DisplayName("존재하지 않는 유저가 좋아요 시 404를 반환한다.")
  void like_실패_유저없음() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(commentLikeService.like(commentId, userId))
        .willThrow(new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound());

    then(commentLikeService).should().like(commentId, userId);
  }

  @Test
  @DisplayName("중복 좋아요 시 409를 반환한다.")
  void like_실패_중복좋아요() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(commentLikeService.like(commentId, userId))
        .willThrow(new CommentException(CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS,
            Map.of("commentId", commentId, "userId", userId)));

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isConflict());

    then(commentLikeService).should().like(commentId, userId);
  }

  @Test
  @DisplayName("Monew-Request-User-ID 헤더 누락 시 401을 반환한다.")
  void like_실패_헤더누락() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId))
        .andExpect(status().isUnauthorized());

    then(commentLikeService).should(never()).like(commentId, userId);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 성공 시 200을 반환한다.")
  void unlike_성공() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isOk());

    then(commentLikeService).should().unlike(commentId, userId);
  }

  @Test
  @DisplayName("존재하지 않는 댓글에 좋아요 취소 시 404를 반환한다.")
  void unlike_실패_댓글없음() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    willThrow(new CommentException(CommentErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId)))
        .given(commentLikeService).unlike(commentId, userId);

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound());

    then(commentLikeService).should().unlike(commentId, userId);
  }

  @Test
  @DisplayName("좋아요를 누르지 않은 댓글에 좋아요 취소 시 404를 반환한다.")
  void unlike_실패_좋아요없음() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    willThrow(new CommentException(CommentErrorCode.COMMENT_LIKE_NOT_FOUND, Map.of("commentId", commentId)))
        .given(commentLikeService).unlike(commentId, userId);

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId))
        .andExpect(status().isNotFound());

    then(commentLikeService).should().unlike(commentId, userId);
  }

  @Test
  @DisplayName("Monew-Request-User-ID 헤더 누락 시 401을 반환한다.")
  void unlike_실패_헤더누락() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId))
        .andExpect(status().isUnauthorized());

    then(commentLikeService).should(never()).unlike(commentId, userId);
  }
}
