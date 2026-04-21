package com.springboot.monew.comment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.comment.dto.CommentLikeDto;
import com.springboot.monew.comment.service.CommentLikeService;
import java.time.Instant;
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

  @Autowired
  ObjectMapper objectMapper;

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

  // UserNotFoundException, CommentNotFoundException, CommentLikeAlreadyExistsException 등의
  // 실패 테스트 케이스는 서비스에서 테스트하는 게 맞다고 판단

  @Test
  @DisplayName("Monew-Request-User-ID 헤더 누락 시 401을 반환한다")
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
  @DisplayName("댓글 좋아요 취소 성공 시 204를 반환한다")
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

  // UserNotFoundException, CommentNotFoundException, CommentLikeAlreadyExistsException 등의
  // 실패 테스트 케이스는 서비스에서 테스트하는 게 맞다고 판단

  @Test
  @DisplayName("Monew-Request-User-ID 헤더 누락 시 401을 반환한다")
  void unlike_실패_헤더누락() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId))
        .andExpect(status().isUnauthorized());
  }
}