package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.response.UserActivityDto;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserActivityMapper;
import com.springboot.monew.users.repository.UserActivityRepository;
import com.springboot.monew.users.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

  private final UserRepository userRepository;
  private final UserActivityRepository userActivityRepository;
  private final UserActivityMapper userActivityMapper;

  public UserActivityDto findUserActivity(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자 활동 조회 실패: 사용자를 찾을 수 없음 - userId={}", userId);
          return new UserException(
              UserErrorCode.USER_NOT_FOUND,
              Map.of("userId", userId)
          );
        });

    if (user.isDeleted()) {
      log.warn("사용자 활동 조회 실패: 삭제된 사용자 - userId={}", userId);
      throw new UserException(
          UserErrorCode.USER_NOT_FOUND,
          Map.of("userId", userId)
      );
    }

    // MongoDB에 활동 내역 문서가 없으면 빈 배열이 들어간 응답 Dto를 반환하도록 설정
    return userActivityRepository.findById(userId)
        .map(userActivityMapper::toDto)
        .orElseGet(() -> {
          log.info("사용자 활동 문서 없음: 빈 활동 응답 반환 - userId={}", userId);
          return userActivityMapper.toEmptyDto(user);
        });
  }
}
