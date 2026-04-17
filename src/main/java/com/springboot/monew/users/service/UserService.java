package com.springboot.monew.users.service;

import com.springboot.monew.users.dto.UserDto;
import com.springboot.monew.users.dto.UserRegisterRequest;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.mapper.UserMapper;
import com.springboot.monew.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
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

        return userMapper.toDto(savedUser);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(
                    UserErrorCode.DUPLICATE_EMAIL,
                    Map.of("email", email)
            );
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new UserException(
                    UserErrorCode.DUPLICATE_NICKNAME,
                    Map.of("nickname", nickname)
            );
        }
    }
}
