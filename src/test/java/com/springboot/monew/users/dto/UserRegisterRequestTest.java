package com.springboot.monew.users.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRegisterRequestTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("이메일, 닉네임, 비밀번호가 모두 올바르면 검증에 성공한다")
    void validateEmail_success() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 아니면 검증에 실패한다")
    void validateEmail_fail_whenInvalidFormat() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("invalid-email", "monew123", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("이메일에 공백이 포함되면 검증에 실패한다")
    void validateEmail_fail_whenContainsWhitespace() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test @example.com", "monew123", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("이메일 길이가 5자 미만이면 검증에 실패한다")
    void validateEmail_fail_whenTooShort() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("a@b", "monew123", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("이메일 길이가 100자를 초과하면 검증에 실패한다")
    void validateEmail_fail_whenTooLong() {
        // given
        String longEmail = "a".repeat(95) + "@test.com";
        UserRegisterRequest request =
                new UserRegisterRequest(longEmail, "monew123", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("닉네임이 null이면 검증에 실패한다")
    void validateNickname_fail_whenNull() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", null, "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("닉네임이 비어 있으면 검증에 실패한다")
    void validateNickname_fail_whenBlank() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("닉네임이 공백만 있으면 검증에 실패한다")
    void validateNickname_fail_whenOnlyWhiteSpace() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "   ", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("닉네임에 공백이 포함되면 검증에 실패한다")
    void validateNickname_fail_whenContainsWhitespace() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "ab c", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("닉네임에 특수문자가 포함되면 검증에 실패한다")
    void validateNickname_fail_whenContainsSpecialCharacter() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "ab@c", "ab12!@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호가 null이면 검증에 실패한다")
    void validatePassword_fail_whenNull() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", null);

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호가 비어 있으면 검증에 실패한다")
    void validatePassword_fail_whenBlank() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호 길이가 6자 미만이면 검증에 실패한다")
    void validatePassword_fail_whenTooShort() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "a1!b2");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호 길이가 20자를 초과하면 검증에 실패한다")
    void validatePassword_fail_whenTooLong() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "abcd1234!@#$efgh5678x");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호에 영문이 없으면 검증에 실패한다")
    void validatePassword_fail_whenNoAlphabet() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "123456!");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 검증에 실패한다")
    void validatePassword_fail_whenNoNumber() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "abcdef!");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호에 특수문자가 없으면 검증에 실패한다")
    void validatePassword_fail_whenNoSpecialCharacter() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "abc123");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("비밀번호에 공백이 포함되면 검증에 실패한다")
    void validatePassword_fail_whenContainsWhitespace() {
        // given
        UserRegisterRequest request =
                new UserRegisterRequest("test@example.com", "monew123", "ab12 !@");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }
}
