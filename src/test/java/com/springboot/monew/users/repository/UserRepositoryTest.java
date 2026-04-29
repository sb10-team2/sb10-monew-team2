package com.springboot.monew.users.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("cutoff 이전에 삭제된 사용자만 조회한다")
  void findUsersDeletedBefore_ReturnsUsersDeletedBeforeCutoff() {
    // given
    Instant cutoff = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    User target = saveUser("target@test.com", "target");
    User excludedAfter = saveUser("after@test.com", "after");

    setDeletedAt(target.getId(), cutoff.minus(1, ChronoUnit.DAYS));
    setDeletedAt(excludedAfter.getId(), cutoff.plus(1, ChronoUnit.HOURS));
    flushAndClear();

    // when
    List<User> result = userRepository.findUsersDeletedBefore(cutoff);

    // then
    assertThat(result).hasSize(1);
    assertThat(result).extracting(User::getEmail)
        .containsExactly("target@test.com");
  }

  @Test
  @DisplayName("삭제 시각이 cutoff와 같아도 조회한다")
  void findUsersDeletedBefore_ReturnsUsersWhenDeletedAtEqualsCutoff() {
    // given
    Instant cutoff = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    User target = saveUser("equal@test.com", "equal");
    setDeletedAt(target.getId(), cutoff);
    flushAndClear();

    // when
    List<User> result = userRepository.findUsersDeletedBefore(cutoff);

    // then
    assertThat(result).extracting(User::getEmail)
        .containsExactly("equal@test.com");
  }

  private User saveUser(String email, String nickname) {
    // 테스트에서 사용할 사용자를 생성하고 즉시 DB에 반영한다.
    User user = new User(email, nickname, "password");
    em.persist(user);
    em.flush();
    return user;
  }

  private void setDeletedAt(UUID userId, Instant deletedAt) {
    // 삭제 시점을 원하는 값으로 맞추기 위해 deleted_at 컬럼을 DB에서 직접 갱신한다.
    em.createNativeQuery("UPDATE users SET deleted_at = :deletedAt WHERE id = :id")
        .setParameter("deletedAt", deletedAt)
        .setParameter("id", userId)
        .executeUpdate();
  }

}
