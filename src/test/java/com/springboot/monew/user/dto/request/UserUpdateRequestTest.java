package com.springboot.monew.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserUpdateRequestTest {

  private static ValidatorFactory factory;
  private Validator validator;

  @BeforeAll
  static void setUpFactory() {
    factory = Validation.buildDefaultValidatorFactory();
  }

  @BeforeEach
  void setUp() {
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  @DisplayName("닉네임 형태가 올바르면 검증에 성공한다")
  void validateNickname_success() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("모뉴monew123");

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("닉네임이 null이면 검증에 실패한다")
  void validateNickname_fail_whenNull() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest(null);

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("닉네임이 비어 있으면 검증에 실패한다")
  void validateNickname_fail_whenBlank() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("");

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("닉네임이 공백만 있으면 검증에 실패한다")
  void validateNickname_fail_whenOnlyWhiteSpace() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("   ");

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("닉네임에 공백이 포함되면 검증에 실패한다")
  void validateNickname_fail_whenContainsWhitespace() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("ab c");

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("닉네임에 특수문자가 포함되면 검증에 실패한다")
  void validateNickname_fail_whenContainsSpecialCharacter() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("ab@c");

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }

  @Test
  @DisplayName("닉네임이 20자를 초과하면 검증에 실패한다")
  void validateNickname_fail_whenTooLong() {
    // given
    UserUpdateRequest request =
        new UserUpdateRequest("a".repeat(21));

    // when
    Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
  }
}
