package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentPageRequest;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.dto.CursorPageResponseCommentDto;
import com.springboot.monew.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface CommentApiDocs {

  @Operation(
      summary = "댓글 목록 조회",
      description = """
          `GET /api/comments`

          커서 기반 페이지네이션으로 댓글 목록을 조회합니다.""",
      operationId = "list_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CursorPageResponseCommentDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 쿼리 파라미터 누락 또는 형식 오류
              - limit이 1 미만이거나 허용 범위 초과
              - orderBy 값이 허용된 값이 아님 (createdAt, likeCount)
              - after 커서 형식 불일치""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  CursorPageResponseCommentDto<CommentDto> list(
      @Valid CommentPageRequest request,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "댓글 등록",
      description = """
          `POST /api/comments`

          새로운 댓글을 등록합니다.""",
      operationId = "create_2"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "댓글 등록 성공",
          content = @Content(schema = @Schema(implementation = CommentDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - content가 null이거나 빈 문자열
              - content 길이 초과
              - articleId 또는 userId 형식 오류 (UUID 아님)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 뉴스 기사를 찾을 수 없음 (NA01)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "415",
          description = "지원하지 않는 Content-Type (application/json 필요)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<CommentDto> create(
      @Valid @RequestBody CommentRegisterRequest request
  );

  @Operation(
      summary = "댓글 논리 삭제",
      description = """
          `DELETE /api/comments/{commentId}`

          댓글을 논리적으로 삭제합니다. 이미 삭제된 댓글은 삭제할 수 없습니다.""",
      operationId = "softDelete_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "댓글 논리 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 삭제된 댓글입니다 (CM05)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "댓글을 찾을 수 없음 (CM01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> softDelete(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId
  );

  @Operation(
      summary = "댓글 수정",
      description = """
          `PATCH /api/comments/{commentId}`

          댓글 내용을 수정합니다. 본인의 댓글만 수정할 수 있습니다.""",
      operationId = "update_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 수정 성공",
          content = @Content(schema = @Schema(implementation = CommentDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - content가 null이거나 빈 문자열
              - content 길이 초과
              - 이미 삭제된 댓글입니다 (CM05)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "본인의 댓글이 아닙니다 (CM02)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "댓글을 찾을 수 없음 (CM01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "415",
          description = "지원하지 않는 Content-Type (application/json 필요)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<CommentDto> update(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Valid @RequestBody CommentUpdateRequest request
  );

  @Operation(
      summary = "댓글 물리 삭제",
      description = """
          `DELETE /api/comments/{commentId}/hard`

          댓글을 물리적으로 삭제합니다. 데이터베이스에서 완전히 제거됩니다.""",
      operationId = "hardDelete_2"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "댓글 물리 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "댓글을 찾을 수 없음 (CM01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> hardDelete(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId
  );
}
