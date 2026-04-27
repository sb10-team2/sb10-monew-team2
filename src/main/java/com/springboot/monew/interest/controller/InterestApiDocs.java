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

public interface InterestApiDocs {

  @Operation(
      summary = "관심사 목록 조회",
      description = """
          `GET /api/interests`

          커서 기반 페이지네이션으로 관심사 목록을 조회합니다.""",
      operationId = "list_2"
  )
  @Parameters({
      @Parameter(
          name = "keyword",
          description = "관심사 이름 검색 키워드 (선택, 부분 일치)",
          example = "인공지능"
      ),
      @Parameter(
          name = "orderBy",
          description = """
              정렬 기준 (필수)
              - `name` : 관심사 이름 순
              - `subscriberCount` : 구독자 수 순""",
          required = true,
          schema = @Schema(type = "string", allowableValues = {"name", "subscriberCount"})
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
              - orderBy=`name` 형식: `인공지능`
              - orderBy=`subscriberCount` 형식: `1000`""",
          example = "인공지능"
      ),
      @Parameter(
          name = "after",
          description = "이전 페이지의 마지막 항목 createdAt (cursor와 함께 사용)",
          example = "2024-01-15T10:30:00.000Z"
      ),
      @Parameter(
          name = "limit",
          description = "페이지당 조회 개수 (필수, 최소: 1, 최대: 100)",
          required = true,
          schema = @Schema(type = "integer", minimum = "1", maximum = "100")
      )
  })
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "관심사 목록 조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CursorPageResponseInterestDto.class),
              examples = @ExampleObject(value = """
                  {
                    "content": [
                      {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "name": "인공지능",
                        "keywords": ["AI", "머신러닝", "딥러닝"],
                        "subscriberCount": 1024,
                        "subscribedByMe": true,
                        "createdAt": "2024-01-15T10:30:00.000Z"
                      }
                    ],
                    "nextCursor": "인공지능",
                    "nextAfter": "2024-01-15T10:30:00.000Z",
                    "size": 1,
                    "totalElements": 15,
                    "hasNext": false
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 파라미터 누락 또는 형식 오류
              - limit 범위 초과 (1~100)
              - orderBy 값이 허용된 값이 아님 (name, subscriberCount)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "BAD_REQUEST",
                    "message": "입력값 검증에 실패하였습니다.",
                    "details": {
                      "orderBy": ["널이어서는 안됩니다"],
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
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InterestDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "인공지능",
                    "keywords": ["AI", "머신러닝", "딥러닝"],
                    "subscriberCount": 0,
                    "subscribedByMe": false,
                    "createdAt": "2024-01-15T10:30:00.000Z"
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - name이 null이거나 빈 문자열
              - keywords가 null이거나 비어 있음
              - keywords에 중복된 값 존재 (IN02)""",
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
                          "name": ["공백일 수 없습니다"],
                          "keywords": ["비어 있을 수 없습니다"]
                        },
                        "exceptionType": "MethodArgumentNotValidException",
                        "status": 400
                      }"""),
                  @ExampleObject(name = "duplicateKeywords", summary = "중복 키워드 (IN02)", value = """
                      {
                        "timestamp": "2024-01-15T10:30:00.000Z",
                        "code": "IN02",
                        "message": "키워드는 중복될 수 없습니다.",
                        "details": {
                          "duplicatedKeyword": "AI"
                        },
                        "exceptionType": "InterestException",
                        "status": 400
                      }""")
              }
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "유사한 이름의 관심사가 이미 존재 (IN01)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN01",
                    "message": "유사한 이름의 관심사가 이미 존재합니다.",
                    "details": {
                      "inputName": "인공 지능",
                      "existingName": "인공지능"
                    },
                    "exceptionType": "InterestException",
                    "status": 409
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
  ResponseEntity<InterestDto> create(
      @Valid @RequestBody InterestRegisterRequest request
  );

  @Operation(
      summary = "관심사 수정",
      description = """
          `PATCH /api/interests/{interestId}`

          관심사의 키워드 목록을 수정합니다.""",
      operationId = "update_2"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "관심사 수정 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InterestDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "name": "인공지능",
                    "keywords": ["AI", "ChatGPT", "LLM"],
                    "subscriberCount": 1024,
                    "subscribedByMe": true,
                    "createdAt": "2024-01-15T10:30:00.000Z"
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - keywords가 null이거나 비어 있음
              - keywords에 중복된 값 존재 (IN02)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN02",
                    "message": "키워드는 중복될 수 없습니다.",
                    "details": {
                      "duplicatedKeyword": "AI"
                    },
                    "exceptionType": "InterestException",
                    "status": 400
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "관심사를 찾을 수 없음 (IN03)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN03",
                    "message": "관심사를 찾을 수 없습니다.",
                    "details": {
                      "interestId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "InterestException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "유사한 이름의 관심사가 이미 존재 (IN01)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN01",
                    "message": "유사한 이름의 관심사가 이미 존재합니다.",
                    "details": {
                      "inputName": "인공 지능",
                      "existingName": "인공지능"
                    },
                    "exceptionType": "InterestException",
                    "status": 409
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
      @ApiResponse(responseCode = "204", description = "관심사 삭제 성공"),
      @ApiResponse(
          responseCode = "404",
          description = "관심사를 찾을 수 없음 (IN03)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN03",
                    "message": "관심사를 찾을 수 없습니다.",
                    "details": {
                      "interestId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "InterestException",
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
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "bb0e8400-e29b-41d4-a716-446655440000",
                    "interestId": "550e8400-e29b-41d4-a716-446655440000",
                    "interestName": "인공지능",
                    "interestKeywords": ["AI", "머신러닝", "딥러닝"],
                    "createdAt": "2024-01-15T10:30:00.000Z"
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
          responseCode = "404",
          description = """
              리소스 없음
              - 관심사를 찾을 수 없음 (IN03)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN03",
                    "message": "관심사를 찾을 수 없습니다.",
                    "details": {
                      "interestId": "550e8400-e29b-41d4-a716-446655440000"
                    },
                    "exceptionType": "InterestException",
                    "status": 404
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 구독 중인 관심사 (IN04)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "IN04",
                    "message": "이미 구독한 관심사입니다.",
                    "details": {
                      "interestId": "550e8400-e29b-41d4-a716-446655440000",
                      "userId": "770e8400-e29b-41d4-a716-446655440002"
                    },
                    "exceptionType": "InterestException",
                    "status": 409
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
      @ApiResponse(responseCode = "204", description = "관심사 구독 취소 성공"),
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
          responseCode = "404",
          description = """
              리소스 없음
              - 관심사를 찾을 수 없음 (IN03)
              - 구독 중인 관심사가 아님 (IN05)""",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = {
                  @ExampleObject(name = "interestNotFound", summary = "관심사 없음 (IN03)", value = """
                      {
                        "timestamp": "2024-01-15T10:30:00.000Z",
                        "code": "IN03",
                        "message": "관심사를 찾을 수 없습니다.",
                        "details": {
                          "interestId": "550e8400-e29b-41d4-a716-446655440000"
                        },
                        "exceptionType": "InterestException",
                        "status": 404
                      }"""),
                  @ExampleObject(name = "subscriptionNotFound", summary = "미구독 관심사 (IN05)", value = """
                      {
                        "timestamp": "2024-01-15T10:30:00.000Z",
                        "code": "IN05",
                        "message": "구독 중인 관심사가 아닙니다.",
                        "details": {
                          "interestId": "550e8400-e29b-41d4-a716-446655440000",
                          "userId": "770e8400-e29b-41d4-a716-446655440002"
                        },
                        "exceptionType": "InterestException",
                        "status": 404
                      }""")
              }
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
  ResponseEntity<Void> unsubscribe(
      @Parameter(description = "관심사 ID") @PathVariable UUID interestId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );
}
