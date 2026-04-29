package com.springboot.monew.user.controller;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.user.dto.response.UserActivityDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface UserActivityDocs {

  @Operation(
      summary = "사용자 활동 내역 조회",
      description = """
          `GET /api/user-activities/{userId}`

          사용자 ID로 활동 내역(구독, 댓글, 좋아요, 기사 조회 이력)을 조회합니다.""",
      operationId = "getUserActivity"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "사용자 활동 내역 조회 성공",
          content = @Content(schema = @Schema(implementation = UserActivityDto.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "사용자를 찾을 수 없음 (UR03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<UserActivityDto> getUserActivity(
      @Parameter(description = "사용자 ID") @PathVariable UUID userId
  );
}
