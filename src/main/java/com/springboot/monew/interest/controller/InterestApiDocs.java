package com.springboot.monew.interest.controller;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.dto.response.SubscriptionDto;
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

public interface InterestApiDocs {

  @Operation(
      summary = "관심사 목록 조회",
      description = """
          `GET /api/interests`

          커서 기반 페이지네이션으로 관심사 목록을 조회합니다.""",
      operationId = "list_2"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "관심사 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CursorPageResponseInterestDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 쿼리 파라미터 누락 또는 형식 오류
              - limit이 1 미만이거나 허용 범위 초과
              - orderBy 값이 허용된 값이 아님 (name, subscriberCount)
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
  CursorPageResponseInterestDto list(
      @Valid InterestPageRequest request,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "관심사 등록",
      description = """
          `POST /api/interests`

          새로운 관심사를 등록합니다. 유사한 이름의 관심사가 이미 존재하면 등록할 수 없습니다.""",
      operationId = "create_3"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "관심사 등록 성공",
          content = @Content(schema = @Schema(implementation = InterestDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - name이 null이거나 빈 문자열
              - name 길이 초과
              - keywords가 null이거나 비어 있음
              - keywords 목록에 중복된 키워드가 존재 (IN02)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "유사한 이름의 관심사가 이미 존재합니다 (IN01)",
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
  ResponseEntity<InterestDto> create(
      @Valid @RequestBody InterestRegisterRequest request
  );

  @Operation(
      summary = "관심사 수정",
      description = """
          `PATCH /api/interests/{interestId}`

          관심사의 키워드 목록을 수정합니다. 유사한 이름의 관심사가 이미 존재하면 수정할 수 없습니다.""",
      operationId = "update_2"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "관심사 수정 성공",
          content = @Content(schema = @Schema(implementation = InterestDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - keywords가 null이거나 비어 있음
              - keywords 목록에 중복된 키워드가 존재 (IN02)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "관심사를 찾을 수 없음 (IN03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "유사한 이름의 관심사가 이미 존재합니다 (IN01)",
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
  ResponseEntity<InterestDto> update(
      @Parameter(description = "관심사 ID") @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request
  );

  @Operation(
      summary = "관심사 물리 삭제",
      description = """
          `DELETE /api/interests/{interestId}`

          관심사를 물리적으로 삭제합니다. 연관된 구독 정보도 함께 삭제됩니다.""",
      operationId = "delete_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "관심사 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "관심사를 찾을 수 없음 (IN03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> delete(
      @Parameter(description = "관심사 ID") @PathVariable UUID interestId
  );

  @Operation(
      summary = "관심사 구독",
      description = """
          `POST /api/interests/{interestId}/subscriptions`

          관심사를 구독합니다. 이미 구독 중인 관심사는 중복 구독할 수 없습니다.""",
      operationId = "subscribe"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "관심사 구독 성공",
          content = @Content(schema = @Schema(implementation = SubscriptionDto.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 관심사를 찾을 수 없음 (IN03)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 구독 중인 관심사입니다 (IN04)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<SubscriptionDto> subscribe(
      @Parameter(description = "관심사 ID") @PathVariable UUID interestId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "관심사 구독 취소",
      description = """
          `DELETE /api/interests/{interestId}/subscriptions`

          관심사 구독을 취소합니다. 구독 중이지 않은 관심사는 취소할 수 없습니다.""",
      operationId = "unsubscribe"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "관심사 구독 취소 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = """
              리소스 없음
              - 관심사를 찾을 수 없음 (IN03)
              - 구독 중인 관심사가 아닙니다 (IN05)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> unsubscribe(
      @Parameter(description = "관심사 ID") @PathVariable UUID interestId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );
}
