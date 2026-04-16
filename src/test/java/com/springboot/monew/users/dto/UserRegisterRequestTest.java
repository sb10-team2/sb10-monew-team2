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
    @DisplayName("올바른 이메일이면 검증에 성공한다")
    void validateEmail_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("test@example.com");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 아니면 검증에 실패한다")
    void validateEmail_fail_whenInvalidFormat() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("invalid-email");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("이메일에 공백이 포함되면 검증에 실패한다")
    void validateEmail_fail_whenContainsWhitespace() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("test @example.com");

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("이메일 길이가 5자 미만이면 검증에 실패한다")
    void validateEmail_fail_whenTooShort() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("a@b");

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
        UserRegisterRequest request = new UserRegisterRequest(longEmail);

        // when
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }
}
