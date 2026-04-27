package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
      @ApiResponse(
          responseCode = "200",
          description = "뉴스 수집 완료"
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류 (외부 API 호출 실패 또는 저장 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
          content = @Content(schema = @Schema(implementation = NewsArticleViewDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 조회한 뉴스 기사입니다 (NA03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
              - 뉴스 기사를 찾을 수 없음 (NA01)
              - 사용자를 찾을 수 없음 (UR03)""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "뉴스 기사 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CursorPageResponseNewsArticleDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - 필수 쿼리 파라미터 누락 또는 형식 오류
              - limit이 1 미만이거나 허용 범위 초과
              - orderBy 값이 허용된 값이 아님 (publishedAt, commentCount)
              - after 커서 형식 불일치
              - source 값이 허용된 출처가 아님""",
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
          content = @Content(schema = @Schema(implementation = NewsArticleDto.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "Monew-Request-User-ID 헤더 누락",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
      @ApiResponse(
          responseCode = "200",
          description = "출처 목록 조회 성공"
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
      @ApiResponse(
          responseCode = "204",
          description = "뉴스 기사 논리 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 삭제된 뉴스 기사입니다 (NA02)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
      @ApiResponse(
          responseCode = "204",
          description = "뉴스 기사 물리 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "뉴스 기사를 찾을 수 없음 (NA01)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> hardDelete(
      @Parameter(description = "뉴스 기사 ID") @PathVariable("articleId") UUID articleId
  );
}
