package com.springboot.monew.notification.controller;

import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface NotificationApiDocs {

  @Operation(
      summary = "알림 목록 조회",
      description = """
          `GET /api/notifications`

          커서 기반 페이지네이션으로 알림 목록을 조회합니다.""",
      operationId = "find"
  )
  @Parameters({
      @Parameter(
          name = "cursor",
          description = "이전 페이지의 마지막 알림 ID (첫 페이지 조회 시 생략)",
          example = "550e8400-e29b-41d4-a716-446655440000",
          schema = @Schema(type = "string", format = "uuid")
      ),
      @Parameter(
          name = "after",
          description = "이전 페이지의 마지막 알림 createdAt (cursor와 함께 사용)",
          example = "2024-01-15T10:30:00.000Z"
      ),
      @Parameter(
          name = "limit",
          description = "페이지당 조회 개수 (필수, 최소: 1)",
          required = true,
          schema = @Schema(type = "integer", minimum = "1")
      )
  })
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "알림 목록 조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CursorPageResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "content": [
                      {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "userId": "770e8400-e29b-41d4-a716-446655440002",
                        "resourceType": "COMMENT",
                        "resourceId": "660e8400-e29b-41d4-a716-446655440001",
                        "message": "홍길동님이 회원님의 댓글에 좋아요를 눌렀습니다.",
                        "confirmed": false,
                        "createdAt": "2024-01-15T10:30:00.000Z",
                        "updatedAt": "2024-01-15T10:30:00.000Z"
                      }
                    ],
                    "nextCursor": "550e8400-e29b-41d4-a716-446655440000",
                    "nextAfter": "2024-01-15T10:30:00.000Z",
                    "size": 1,
                    "totalElements": 5,
                    "hasNext": false
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - limit 누락 또는 1 미만""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "BAD_REQUEST",
                    "message": "입력값 검증에 실패하였습니다.",
                    "details": {
                      "limit": ["1 이상이어야 합니다"]
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
          description = """
              서버 내부 오류
              - DB 알림 데이터 손상 (NT01)
              - 알림 타입과 연결 객체 불일치 (NT02)
              - 알림 생성 필수 객체 누락 (NT03)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NT01",
                    "message": "DB에 저장된 알림 데이터가 손상되었거나 유효하지 않습니다.",
                    "details": {
                      "notificationId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "NotificationException",
                    "status": 500
                  }""")
          )
      )
  })
  ResponseEntity<CursorPageResponse<NotificationDto>> find(
      @Valid NotificationFindRequest request,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId
  );

  @Operation(
      summary = "알림 전체 확인 처리",
      description = """
          `PATCH /api/notifications`

          사용자의 모든 미확인 알림을 일괄 확인 처리합니다.""",
      operationId = "bulkUpdate"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "알림 전체 확인 처리 성공"),
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
  ResponseEntity<?> bulkUpdate(
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId
  );

  @Operation(
      summary = "알림 단건 확인 처리",
      description = """
          `PATCH /api/notifications/{notificationId}`

          특정 알림을 확인 처리합니다. 이미 확인된 알림은 다시 확인 처리할 수 없습니다.""",
      operationId = "update_3"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "알림 확인 처리 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "이미 확인 처리된 알림 (NT04)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NT04",
                    "message": "이미 읽음 처리된 알림입니다.",
                    "details": {
                      "notificationId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "NotificationException",
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
          description = """
              서버 내부 오류
              - 알림을 찾을 수 없음 (NT05)
              - DB 알림 데이터 손상 (NT01)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NT05",
                    "message": "알림을 찾을 수 없습니다.",
                    "details": {
                      "notificationId": "550e8400-e29b-41d4-a716-446655440000",
                      "userId": "770e8400-e29b-41d4-a716-446655440002"
                    },
                    "exceptionType": "NotificationException",
                    "status": 500
                  }""")
          )
      )
  })
  ResponseEntity<?> update(
      @Parameter(description = "알림 ID") @PathVariable @NotNull UUID notificationId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId
  );
}
