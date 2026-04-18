package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserLoginRequest;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserMapper;
import com.springboot.monew.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto register(UserRegisterRequest request) {
        validateDuplicateEmail(request.email());
        validateDuplicateNickname(request.nickname());

        User user = new User(
                request.email(),
                request.nickname(),
                request.password()
        );

        User savedUser = userRepository.save(user);
        log.info("[User] 회원가입 완료 - userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    public UserDto login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("[User] 로그인 실패: 사용자를 찾을 수 없음 - email={}", request.email());
                    return new UserException(
                            UserErrorCode.USER_NOT_FOUND,
                            Map.of("email", request.email())
                    );
                });

        if (user.isDeleted()) {
            log.warn("[User] 로그인 실패: 탈퇴한 사용자 - email={}", request.email());
            throw new UserException(
                    UserErrorCode.USER_NOT_FOUND,
                    Map.of("email", request.email())
            );
        }

        if (!user.getPassword().equals(request.password())) {
            log.warn("[User] 로그인 실패: 비밀번호 불일치 - email={}", request.email());
            throw new UserException(
                    UserErrorCode.INVALID_CREDENTIALS,
                    Map.of("email", request.email())
            );
        }

        log.info("[User] 로그인 완료 - userId={}, email={}", user.getId(), user.getEmail());
        return userMapper.toDto(user);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("[User] 회원가입 실패 - email={}", email);
            throw new UserException(
                    UserErrorCode.DUPLICATE_EMAIL,
                    Map.of("email", email)
            );
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            log.warn("[User] 회원가입 실패 - nickname={}", nickname);
            throw new UserException(
                    UserErrorCode.DUPLICATE_NICKNAME,
                    Map.of("nickname", nickname)
            );
        }
    }
}
