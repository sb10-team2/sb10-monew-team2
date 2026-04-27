package com.springboot.monew.notification.controller;

import com.springboot.monew.common.dto.CursorPageResponse;
import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.notification.dto.NotificationDto;
import com.springboot.monew.notification.dto.NotificationFindRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "알림 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CursorPageResponse.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 쿼리 파라미터 누락 또는 형식 오류
              - limit이 1 미만이거나 허용 범위 초과
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
          description = """
              서버 내부 오류
              - DB에 저장된 알림 데이터가 손상되었거나 유효하지 않음 (NT01)
              - 알림 타입과 연결된 객체가 일치하지 않음 (NT02)
              - 알림 생성에 필요한 필수 객체가 누락됨 (NT03)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
      @ApiResponse(
          responseCode = "204",
          description = "알림 전체 확인 처리 성공"
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
      @ApiResponse(
          responseCode = "204",
          description = "알림 확인 처리 성공"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 확인 처리된 알림입니다 (NT04)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = """
              서버 내부 오류
              - 알림을 찾을 수 없음 (NT05)
              - DB에 저장된 알림 데이터가 손상되었거나 유효하지 않음 (NT01)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<?> update(
      @Parameter(description = "알림 ID") @PathVariable @NotNull UUID notificationId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") @NotNull UUID userId
  );
}
