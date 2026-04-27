package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface NewsArticleApiDocs {

  @Operation(
      summary = "뉴스 기사 수집",
      description = """
          `POST /api/articles`

          외부 출처(네이버, RSS 등)에서 관심사 키워드 기반으로 뉴스 기사를 수집합니다.""",
      operationId = "collectNews"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "뉴스 수집 완료"),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류 (외부 API 호출 실패 또는 저장 실패)",
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
  ResponseEntity<String> collectNews();

  @Operation(
      summary = "뉴스 기사 조회 이력 등록",
      description = """
          `POST /api/articles/{articleId}/article-views`

          사용자가 뉴스 기사를 조회했음을 기록합니다. 동일한 기사를 이미 조회한 경우 중복 등록할 수 없습니다.""",
      operationId = "createView"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "뉴스 기사 조회 이력 등록 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = NewsArticleViewDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "cc0e8400-e29b-41d4-a716-446655440000",
                    "articleId": "660e8400-e29b-41d4-a716-446655440001",
                    "userId": "770e8400-e29b-41d4-a716-446655440002",
                    "createdAt": "2024-01-15T10:30:00.000Z"
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 조회한 뉴스 기사 (NA03)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NA03",
                    "message": "이미 조회된 뉴스기사입니다.",
                    "details": {
                      "articleId": "660e8400-e29b-41d4-a716-446655440001",
                      "userId": "770e8400-e29b-41d4-a716-446655440002"
                    },
                    "exceptionType": "ArticleException",
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
  ResponseEntity<NewsArticleViewDto> createView(
      @Parameter(description = "뉴스 기사 ID") @PathVariable("articleId") UUID articleId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "뉴스 기사 목록 조회",
      description = """
          `GET /api/articles`

          커서 기반 페이지네이션으로 뉴스 기사 목록을 조회합니다.""",
      operationId = "list_3"
  )
  @Parameters({
      @Parameter(
          name = "keyword",
          description = "기사 제목/요약 검색 키워드 (선택, 부분 일치)",
          example = "인공지능"
      ),
      @Parameter(
          name = "interestId",
          description = "특정 관심사에 연관된 기사만 조회 (선택)",
          example = "550e8400-e29b-41d4-a716-446655440000",
          schema = @Schema(type = "string", format = "uuid")
      ),
      @Parameter(
          name = "sourceIn",
          description = """
              출처 필터 (선택, 복수 선택 가능)
              - `NAVER` : 네이버
              - `HANKYUNG` : 한국경제
              - `CHOSUN` : 조선일보
              - `YONHAP` : 연합뉴스""",
          schema = @Schema(type = "array", allowableValues = {"NAVER", "HANKYUNG", "CHOSUN", "YONHAP"})
      ),
      @Parameter(
          name = "publishDateFrom",
          description = "발행일 범위 시작 (선택, ISO 8601 형식)",
          example = "2024-01-01T00:00:00.000Z"
      ),
      @Parameter(
          name = "publishDateTo",
          description = "발행일 범위 종료 (선택, ISO 8601 형식)",
          example = "2024-01-31T23:59:59.000Z"
      ),
      @Parameter(
          name = "orderBy",
          description = """
              정렬 기준 (필수)
              - `publishDate` : 발행일 순
              - `commentCount` : 댓글 수 순
              - `viewCount` : 조회 수 순""",
          required = true,
          schema = @Schema(type = "string", allowableValues = {"publishDate", "commentCount", "viewCount"})
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
              - orderBy=`publishDate` 형식: `2024-01-15T10:30:00.000000000Z`
              - orderBy=`commentCount` / `viewCount` 형식: `100|2024-01-15T10:30:00.000000000Z`""",
          example = "2024-01-15T10:30:00.000000000Z"
      ),
      @Parameter(
          name = "after",
          description = "이전 페이지의 마지막 항목 publishDate (cursor와 함께 사용)",
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
          description = "뉴스 기사 목록 조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CursorPageResponseNewsArticleDto.class),
              examples = @ExampleObject(value = """
                  {
                    "content": [
                      {
                        "id": "660e8400-e29b-41d4-a716-446655440001",
                        "source": "NAVER",
                        "sourceUrl": "https://news.naver.com/article/001/0001234567",
                        "title": "AI 기술 혁신, 산업 전반 변화 이끈다",
                        "summary": "인공지능 기술의 급격한 발전으로 제조·금융·의료 등 산업 전반에 걸쳐 대규모 변화가 예고되고 있다.",
                        "imageUrl": "https://imgnews.pstatic.net/image/001/2024/01/15/sample.jpg",
                        "publishedAt": "2024-01-15T10:30:00.000Z",
                        "commentCount": 42,
                        "viewCount": 1500,
                        "viewedByMe": false
                      }
                    ],
                    "nextCursor": "2024-01-15T10:30:00.000000000Z",
                    "nextAfter": "2024-01-15T10:30:00.000Z",
                    "size": 1,
                    "totalElements": 320,
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
              - orderBy 값이 허용된 값이 아님 (publishDate, commentCount, viewCount)
              - sourceIn에 허용되지 않는 출처 값 포함""",
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
  CursorPageResponseNewsArticleDto list(
      @Valid NewsArticlePageRequest request,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "뉴스 기사 단건 조회",
      description = """
          `GET /api/articles/{articleId}`

          뉴스 기사 ID로 특정 뉴스 기사를 조회합니다.""",
      operationId = "find_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "뉴스 기사 단건 조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = NewsArticleDto.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "660e8400-e29b-41d4-a716-446655440001",
                    "source": "NAVER",
                    "sourceUrl": "https://news.naver.com/article/001/0001234567",
                    "title": "AI 기술 혁신, 산업 전반 변화 이끈다",
                    "summary": "인공지능 기술의 급격한 발전으로 산업 전반에 걸쳐 대규모 변화가 예고되고 있다.",
                    "imageUrl": "https://imgnews.pstatic.net/image/001/2024/01/15/sample.jpg",
                    "publishedAt": "2024-01-15T10:30:00.000Z",
                    "commentCount": 42,
                    "viewCount": 1500,
                    "viewedByMe": true
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
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
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
  ResponseEntity<NewsArticleDto> find(
      @Parameter(description = "뉴스 기사 ID") @PathVariable("articleId") UUID articleId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  );

  @Operation(
      summary = "뉴스 기사 출처 목록 조회",
      description = """
          `GET /api/articles/sources`

          사용 가능한 뉴스 기사 출처 목록을 조회합니다.""",
      operationId = "findSource"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "출처 목록 조회 성공"),
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
  ResponseEntity<List<ArticleSource>> findSource();

  @Operation(
      summary = "뉴스 기사 논리 삭제",
      description = """
          `DELETE /api/articles/{articleId}`

          뉴스 기사를 논리적으로 삭제합니다. 이미 삭제된 기사는 삭제할 수 없습니다.""",
      operationId = "softDelete_2"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "뉴스 기사 논리 삭제 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "이미 삭제된 뉴스 기사 (NA02)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "timestamp": "2024-01-15T10:30:00.000Z",
                    "code": "NA02",
                    "message": "이미 삭제된 뉴스기사입니다.",
                    "details": {
                      "articleId": "660e8400-e29b-41d4-a716-446655440001"
                    },
                    "exceptionType": "ArticleException",
                    "status": 400
                  }""")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
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
      @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId
  );

  @Operation(
      summary = "뉴스 기사 물리 삭제",
      description = """
          `DELETE /api/articles/{articleId}/hard`

          뉴스 기사를 물리적으로 삭제합니다. 데이터베이스에서 완전히 제거됩니다.""",
      operationId = "hardDelete_3"
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "뉴스 기사 물리 삭제 성공"),
      @ApiResponse(
          responseCode = "404",
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
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
      @Parameter(description = "뉴스 기사 ID") @PathVariable("articleId") UUID articleId
  );
}
