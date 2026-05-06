package com.springboot.monew.comment.controller;

import com.springboot.monew.comment.dto.CommentDto;
import com.springboot.monew.comment.dto.CommentPageRequest;
import com.springboot.monew.comment.dto.CommentRegisterRequest;
import com.springboot.monew.comment.dto.CommentUpdateRequest;
import com.springboot.monew.comment.dto.CursorPageResponseCommentDto;
import com.springboot.monew.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
  @Parameters({
      @Parameter(
          name = "articleId",
          description = "조회할 뉴스 기사 ID (필수)",
          required = true,
          example = "550e8400-e29b-41d4-a716-446655440000",
          schema = @Schema(type = "string", format = "uuid")
      ),
      @Parameter(
          name = "orderBy",
          description = """
              정렬 기준 (필수)
              - `createdAt` : 등록일 순
              - `likeCount` : 좋아요 수 순""",
          required = true,
          schema = @Schema(type = "string", allowableValues = {"createdAt", "likeCount"})
      ),
      @Parameter(
          name = "direction",
          description = """
              정렬 방향 (필수)
              - `ASC` : 오름차순
              - `DESC` : 내림차순""",
          required = true,
          schema = @Schema(type = "string", allowableValues = {"ASC", "DESC"})
      ),
      @Parameter(
          name = "cursor",
          description = """
              이전 페이지의 마지막 커서 값 (첫 페이지 조회 시 생략)
              - orderBy=`createdAt` 형식: `2024-01-15T10:30:00.000000000Z`
              - orderBy=`likeCount` 형식: `5|2024-01-15T10:30:00.000000000Z`""",
          example = "2024-01-15T10:30:00.000000000Z"
      ),
      @Parameter(
          name = "after",
          description = "이전 페이지의 마지막 항목 createdAt (cursor와 함께 사용)",
          example = "2024-01-15T10:30:00.000Z"
      ),
      @Parameter(
          name = "limit",
          description = "페이지당 조회 개수 (기본값: 50, 최소: 1, 최대: 100)",
          schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "50")
      )
  })
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 목록 조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CursorPageResponseCommentDto.class),
              examples = @ExampleObject(value = """
                  {
                    "content": [
                      {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "articleId": "660e8400-e29b-41d4-a716-446655440001",
                        "userId": "770e8400-e29b-41d4-a716-446655440002",
                        "userNickname": "홍길동",
                        "content": "좋은 기사네요!",
                        "likeCount": 5,
                        "likedByMe": false,
                        "createdAt": "2024-01-15T10:30:00.000Z"
                      }
                    ],
                    "nextCursor": "2024-01-15T10:30:00.000000000Z",
                    "nextAfter": "2024-01-15T10:30:00.000Z",
                    "size": 1,
                    "totalElements": 42,
                    "hasNext": true
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 파라미터 누락 또는 형식 오류
              - limit 범위 초과 (1~100)
              - orderBy 값이 허용된 값이 아님 (createdAt, likeCount)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "BAD_REQUEST",
                    "message": "입력값 검증에 실패하였습니다.",
                    "details": {
                      "articleId": ["널이어서는 안됩니다"],
                      "limit": ["1에서 100 사이여야 합니다"]
                    },
                    "exceptionType": "MethodArgumentNotValidException",
                    "status": 400
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "UNAUTHORIZED",
                    "message": "Required request header 'Monew-Request-User-ID' for method parameter type UUID is not present",
                    "details": {},
                    "exceptionType": "MissingRequestHeaderException",
                    "status": 401
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                    "details": {},
                    "exceptionType": "Exception",
                    "status": 500
                  }""")
          )
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
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CommentDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "articleId": "660e8400-e29b-41d4-a716-446655440001",
                    "userId": "770e8400-e29b-41d4-a716-446655440002",
                    "userNickname": "홍길동",
                    "content": "좋은 기사네요!",
                    "likeCount": 0,
                    "likedByMe": false,
                    "createdAt": "2024-01-15T10:30:00.000Z"
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - content가 null이거나 빈 문자열
              - articleId 또는 userId 형식 오류 (UUID 아님)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "BAD_REQUEST",
                    "message": "입력값 검증에 실패하였습니다.",
                    "details": {
                      "content": ["공백일 수 없습니다"],
                      "articleId": ["널이어서는 안됩니다"]
                    },
                    "exceptionType": "MethodArgumentNotValidException",
                    "status": 400
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 뉴스 기사를 찾을 수 없음 (NA01)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NA01",
                    "message": "뉴스기사를 찾을 수 없습니다.",
                    "details": {
                      "articleId": "660e8400-e29b-41d4-a716-446655440001"
                    },
                    "exceptionType": "ArticleException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "415",
          description = "지원하지 않는 Content-Type (application/json 필요)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "UNSUPPORTED_MEDIA_TYPE",
                    "message": "Content-Type 'text/plain' is not supported",
                    "details": {},
                    "exceptionType": "HttpMediaTypeNotSupportedException",
                    "status": 415
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                    "details": {},
                    "exceptionType": "Exception",
                    "status": 500
                  }""")
          )
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
          description = "이미 삭제된 댓글 (CM05)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "CM05",
                    "message": "이미 삭제한 댓글입니다.",
                    "details": {
                      "commentId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "CommentException",
                    "status": 400
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "댓글을 찾을 수 없음 (CM01)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "CM01",
                    "message": "댓글을 찾을 수 없습니다.",
                    "details": {
                      "commentId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "CommentException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                    "details": {},
                    "exceptionType": "Exception",
                    "status": 500
                  }""")
          )
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
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CommentDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "articleId": "660e8400-e29b-41d4-a716-446655440001",
                    "userId": "770e8400-e29b-41d4-a716-446655440002",
                    "userNickname": "홍길동",
                    "content": "수정된 댓글 내용입니다.",
                    "likeCount": 3,
                    "likedByMe": true,
                    "createdAt": "2024-01-15T10:30:00.000Z"
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - content가 null이거나 빈 문자열
              - 이미 삭제된 댓글 (CM05)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                  @ExampleObject(name = "validationError", summary = "입력값 검증 실패", value = """
                      {
                        "timestamp": "2024-01-15T10:30:00.000Z",
                        "code": "BAD_REQUEST",
                        "message": "입력값 검증에 실패하였습니다.",
                        "details": {
                          "content": ["공백일 수 없습니다"]
                        },
                        "exceptionType": "MethodArgumentNotValidException",
                        "status": 400
                      }"""),
                  @ExampleObject(name = "alreadyDeleted", summary = "이미 삭제된 댓글 (CM05)", value = """
                      {
                        "timestamp": "2024-01-15T10:30:00.000Z",
                        "code": "CM05",
                        "message": "이미 삭제한 댓글입니다.",
                        "details": {
                          "commentId": "550e8400-e29b-41d4-a716-446655440000"
                        },
                        "exceptionType": "CommentException",
                        "status": 400
                      }""")
              }
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "UNAUTHORIZED",
                    "message": "Required request header 'Monew-Request-User-ID' for method parameter type UUID is not present",
                    "details": {},
                    "exceptionType": "MissingRequestHeaderException",
                    "status": 401
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "본인의 댓글이 아님 (CM02)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "CM02",
                    "message": "본인의 댓글이 아닙니다.",
                    "details": {
                      "commentId": "550e8400-e29b-41d4-a716-446655440000",
                      "userId": "770e8400-e29b-41d4-a716-446655440002"
                    },
                    "exceptionType": "CommentException",
                    "status": 403
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "댓글을 찾을 수 없음 (CM01)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "CM01",
                    "message": "댓글을 찾을 수 없습니다.",
                    "details": {
                      "commentId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "CommentException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "415",
          description = "지원하지 않는 Content-Type (application/json 필요)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "UNSUPPORTED_MEDIA_TYPE",
                    "message": "Content-Type 'text/plain' is not supported",
                    "details": {},
                    "exceptionType": "HttpMediaTypeNotSupportedException",
                    "status": 415
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                    "details": {},
                    "exceptionType": "Exception",
                    "status": 500
                  }""")
          )
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
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "CM01",
                    "message": "댓글을 찾을 수 없습니다.",
                    "details": {
                      "commentId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "CommentException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "서버 내부 오류가 발생했습니다.",
                    "details": {},
                    "exceptionType": "Exception",
                    "status": 500
                  }""")
          )
      )
  })
  ResponseEntity<Void> hardDelete(
      @Parameter(description = "댓글 ID") @PathVariable UUID commentId
  );
}
