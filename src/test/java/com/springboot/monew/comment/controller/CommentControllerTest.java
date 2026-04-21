package com.springboot.monew.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentPageRequest;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.dto.CursorPageResponseCommentDto;
import com.springboot.monew.comment.service.CommentService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  @Test
  @DisplayName("댓글이 등록이 성공하면 201을 반환한다")
  void create_성공() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "테스트 댓글");

    CommentDto response = new CommentDto(
        UUID.randomUUID(),
        articleId,
        userId,
        "테스트 유저",
        "테스트 댓글",
        0L,
        false,
        Instant.now()
        );

    given(commentService.create(request)).willReturn(response);
    // when & then
    mockMvc.perform(post("/api/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()));

  }

  @Test
  @DisplayName("댓글 내용이 없다면 등록에 실패한다")
  void create_실패() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "");

    // when & then
    mockMvc.perform(post("/api/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.content").value("내용을 입력해주세요."));

    // 잘못된 요청일 경우 service 실행 X
    then(commentService).should(never()).create(request);
  }

  @Test
  @DisplayName("댓글 논리 삭제 시 204를 반환한다.")
  void softDelete_성공() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNoContent());

    then(commentService).should().softDelete(commentId);
  }

  // 논리 삭제 실패 테스트 케이스는 서비스 테스트에서 구현할 예정

  @Test
  @DisplayName("댓글 수정 성공 시 200을 반환한다.")
  void update_성공() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CommentUpdateRequest request = new CommentUpdateRequest("수정한 테스트 댓글");

    CommentDto response = new CommentDto(
        UUID.randomUUID(),
        UUID.randomUUID(),
        userId,
        "닉네임",
        "수정한 테스트 댓글",
        1L,
        false,
        Instant.now()
    );
    given(commentService.update(commentId, userId, request)).willReturn(response);
    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Monew-Request-User-ID", userId)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("수정한 테스트 댓글"));

    then(commentService).should().update(commentId, userId, request);
  }

  @Test
  @DisplayName("댓글 수정 실패 시 400을 반환한다.")
  void update_실패() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CommentUpdateRequest request = new CommentUpdateRequest("");

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Monew-Request-User-ID", userId)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.content").value("내용을 입력해주세요."));

    // 잘못된 요청일 경우 service 실행 X
    then(commentService).should(never()).update(commentId, userId, request);
    // then
  }

  @Test
  @DisplayName("댓글 물리 삭제 시 204를 반환한다.")
  void hardDelete_성공() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
        .andExpect(status().isNoContent());

    then(commentService).should().hardDelete(commentId);
  }

  // 물리 삭제 실패 test Case는 비즈니스 로직이라 서비스 테스트에서 해야한다고 판단

  @Test
  @DisplayName("댓글 목록 조회 성공")
  void list_성공() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentDto comment1 = new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "유저1",
        "댓글1", 0L, false, Instant.now());
    CommentDto comment2 = new CommentDto(UUID.randomUUID(), articleId, UUID.randomUUID(), "유저2",
        "댓글2", 1L, true, Instant.now());

    CursorPageResponseCommentDto<CommentDto> response = new CursorPageResponseCommentDto<>(
        List.of(comment1, comment2), null, null, 2, 2, false
    );

    given(commentService.list(any(CommentPageRequest.class), any(UUID.class))).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId)
            .param("articleId", articleId.toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].content").value("댓글1"))
        .andExpect(jsonPath("$.content[1].content").value("댓글2"))
        .andExpect(jsonPath("$.hasNext").value(false));

    then(commentService).should().list(any(CommentPageRequest.class), any(UUID.class));
  }

  @Test
  @DisplayName("댓글 목록 조회 실패 - 잘못된 정렬 속성")
  void list_실패() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId)
            .param("articleId", articleId.toString())
            .param("orderBy", "잘못된 정렬 방향")
            .param("direction", "DESC"))
        .andExpect(status().isBadRequest());

    then(commentService).should(never()).list(any(CommentPageRequest.class), any(UUID.class));
  }
}