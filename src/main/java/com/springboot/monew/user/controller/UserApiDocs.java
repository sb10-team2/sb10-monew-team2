package com.springboot.monew.user.controller;

import com.springboot.monew.common.exception.ErrorResponse;
import com.springboot.monew.user.dto.request.UserLoginRequest;
import com.springboot.monew.user.dto.request.UserRegisterRequest;
import com.springboot.monew.user.dto.request.UserUpdateRequest;
import com.springboot.monew.user.dto.response.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface UserApiDocs {

  @Operation(
      summary = "회원가입",
      description = """
          `POST /api/users`

          새로운 사용자를 등록합니다.""",
      operationId = "register"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "회원가입 성공",
          content = @Content(schema = @Schema(implementation = UserDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - email이 null이거나 이메일 형식 아님
              - password가 null이거나 최소 길이 미달
              - nickname이 null이거나 빈 문자열""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = """
              중복 충돌
              - 이미 사용 중인 이메일입니다 (UR01)
              - 이미 사용 중인 닉네임입니다 (UR02)""",
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
  ResponseEntity<UserDto> register(
      @Valid @RequestBody UserRegisterRequest request
  );

  @Operation(
      summary = "로그인",
      description = """
          `POST /api/users/login`

          사용자 로그인을 처리합니다.""",
      operationId = "login"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그인 성공",
          content = @Content(schema = @Schema(implementation = UserDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - email이 null이거나 이메일 형식 아님
              - password가 null이거나 빈 문자열""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "이메일 또는 비밀번호가 올바르지 않습니다 (UR04)",
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
  ResponseEntity<UserDto> login(
      @Valid @RequestBody UserLoginRequest request
  );

  @Operation(
      summary = "사용자 정보 수정",
      description = """
          `PATCH /api/users/{userId}`

          사용자의 닉네임을 수정합니다. 본인의 계정만 수정할 수 있습니다.""",
      operationId = "update"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "사용자 정보 수정 성공",
          content = @Content(schema = @Schema(implementation = UserDto.class))
      ),
      @ApiResponse(
          responseCode = "400",
          description = """
              잘못된 요청
              - nickname이 null이거나 빈 문자열
              - nickname 길이 초과""",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "403",
          description = "본인의 사용자 정보만 수정할 수 있습니다 (UR05)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "404",
          description = "사용자를 찾을 수 없음 (UR03)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 사용 중인 닉네임입니다 (UR02)",
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
  ResponseEntity<UserDto> update(
      @Parameter(description = "사용자 ID") @PathVariable UUID userId,
      @Valid @RequestBody UserUpdateRequest request
  );

  @Operation(
      summary = "사용자 논리 삭제",
      description = """
          `DELETE /api/users/{userId}`

          사용자를 논리적으로 삭제합니다.""",
      operationId = "delete"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "사용자 논리 삭제 성공"
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
  ResponseEntity<Void> delete(
      @Parameter(description = "사용자 ID") @PathVariable UUID userId
  );

  @Operation(
      summary = "사용자 물리 삭제",
      description = """
          `DELETE /api/users/{userId}/hard`

          사용자를 물리적으로 삭제합니다. 데이터베이스에서 완전히 제거됩니다.""",
      operationId = "hardDelete_1"
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "사용자 물리 삭제 성공"
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
  ResponseEntity<Void> hardDelete(
      @Parameter(description = "사용자 ID") @PathVariable UUID userId
  );
}
