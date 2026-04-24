package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.request.UserLoginRequest;
import com.springboot.monew.users.dto.request.UserRegisterRequest;
import com.springboot.monew.users.dto.request.UserUpdateRequest;
import com.springboot.monew.users.dto.response.UserDto;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.event.user.UserNicknameUpdatedEvent;
import com.springboot.monew.users.event.user.UserRegisteredEvent;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserMapper;
import com.springboot.monew.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final ApplicationEventPublisher applicationEventPublisher;

  // 사용자 회원가입
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
    // 회원가입 후 사용자 활동 문서 생성을 위해 이벤트 발행
    applicationEventPublisher.publishEvent(new UserRegisteredEvent(savedUser));
    log.info("회원가입 완료 - userId={}, email={}", savedUser.getId(), savedUser.getEmail());
    return userMapper.toDto(savedUser);
  }

  // 사용자 로그인
  public UserDto login(UserLoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> {
          log.warn("로그인 실패: 사용자를 찾을 수 없음 - email={}", request.email());
          return new UserException(
              UserErrorCode.USER_NOT_FOUND,
              Map.of("email", request.email())
          );
        });

    if (user.isDeleted()) {
      log.warn("로그인 실패: 탈퇴한 사용자 - email={}", request.email());
      throw new UserException(
          UserErrorCode.USER_NOT_FOUND,
          Map.of("email", request.email())
      );
    }

    if (!user.getPassword().equals(request.password())) {
      log.info("로그인 실패: 비밀번호 불일치 - email={}", request.email());
      throw new UserException(
          UserErrorCode.INVALID_CREDENTIALS,
          Map.of("email", request.email())
      );
    }

    log.info("로그인 완료 - userId={}, email={}", user.getId(), user.getEmail());
    return userMapper.toDto(user);
  }

  // 닉네임 수정
  @Transactional
  public UserDto update(UUID userId, UUID requestUserId, UserUpdateRequest request) {
    // 다른 개발자 도구로 닉네임 수정을 막기 위해 '수정 대상 사용자'와 '요청을 보낸 사용자'가 같은지 검사
    validateOwner(userId, requestUserId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("닉네임 수정 실패: 사용자를 찾을 수 없음 - userId={}", userId);
          return new UserException(
              UserErrorCode.USER_NOT_FOUND,
              Map.of("userId", userId)
          );
        });

    if (user.isDeleted()) {
      log.warn("닉네임 수정 실패: 탈퇴한 사용자 - userId={}", userId);
      throw new UserException(
          UserErrorCode.USER_NOT_FOUND,
          Map.of("userId", userId)
      );
    }

    if (!user.getNickname().equals(request.nickname())
        && userRepository.existsByNickname(request.nickname())) {
      log.warn("닉네임 수정 실패: 닉네임 중복 - nickname={}", request.nickname());
      throw new UserException(
          UserErrorCode.DUPLICATE_NICKNAME,
          Map.of("nickname", request.nickname())
      );
    }

    user.updateNickname(request.nickname());
    // 닉네임 수정 후 사용자 활동 문서 갱신을 위해 이벤트 발행
    applicationEventPublisher.publishEvent(
        new UserNicknameUpdatedEvent(user.getId(), user.getNickname())
    );
    log.info("닉네임 수정 완료 - userId={}, nickname={}", user.getId(), user.getNickname());
    return userMapper.toDto(user);
  }

  // 논리 삭제
  @Transactional
  public void delete(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자 탈퇴 실패: 사용자를 찾을 수 없음 - userId={}", userId);
          return new UserException(
              UserErrorCode.USER_NOT_FOUND,
              Map.of("userId", userId)
          );
        });

    if (user.isDeleted()) {
      log.warn("사용자 탈퇴 실패: 탈퇴한 사용자 - userId={}", userId);
      throw new UserException(
          UserErrorCode.USER_NOT_FOUND,
          Map.of("userId", userId)
      );
    }

    user.delete();
    log.info("사용자 탈퇴 완료 - userId={}", user.getId());
  }

  // 수동 물리 삭제
  @Transactional
  public void hardDelete(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자 물리 삭제 실패: 사용자를 찾을 수 없음 - userId={}", userId);
          return new UserException(
              UserErrorCode.USER_NOT_FOUND,
              Map.of("userId", userId)
          );
        });

    userRepository.delete(user);
    log.info("사용자 물리 삭제 완료 - userId={}", userId);
  }

  // 24시간 후 자동 물리 삭제, 몇 명이 삭제되었는지 확인할 수 있도록 반환 타입을 int로 설정
  @Transactional
  public int purgeDeletedUsersOlderThan(Instant cutoff) {
    List<User> users = userRepository.findUsersDeletedBefore(cutoff);

    if (users.isEmpty()) {
      log.info("물리 삭제 대상 사용자 없음 - cutoff={}", cutoff);
      return 0;
    }

    userRepository.deleteAll(users);

    log.info("논리 삭제 후 24시간 경과 사용자 물리 삭제 완료 - count={}, cutoff={}", users.size(), cutoff);
    return users.size();
  }

  private void validateDuplicateEmail(String email) {
    if (userRepository.existsByEmail(email)) {
      log.warn("회원가입 실패 - email={}", email);
      throw new UserException(
          UserErrorCode.DUPLICATE_EMAIL,
          Map.of("email", email)
      );
    }
  }

  private void validateDuplicateNickname(String nickname) {
    if (userRepository.existsByNickname(nickname)) {
      log.warn("회원가입 실패 - nickname={}", nickname);
      throw new UserException(
          UserErrorCode.DUPLICATE_NICKNAME,
          Map.of("nickname", nickname)
      );
    }
  }

  private void validateOwner(UUID userId, UUID requestUserId) {
    if (!userId.equals(requestUserId)) {
      log.warn("사용자 권한 검증 실패: 사용자 불일치 - userId={}, requestUserId={}", userId, requestUserId);
      throw new UserException(
          UserErrorCode.USER_NOT_OWNED,
          Map.of("userId", userId, "requestUserId", requestUserId)
      );
    }
  }
}
