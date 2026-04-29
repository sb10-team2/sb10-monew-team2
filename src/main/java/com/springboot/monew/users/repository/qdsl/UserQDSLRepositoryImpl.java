package com.springboot.monew.users.repository.qdsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.users.entity.QUser;
import com.springboot.monew.users.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQDSLRepositoryImpl implements UserQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private final QUser qUser = QUser.user;

  @Override
  public List<User> findUsersDeletedBefore(Instant cutoff) {
    Objects.requireNonNull(cutoff, "cutoff는 null일 수 없습니다.");

    return queryFactory
        .selectFrom(qUser)
        .where(
            // 삭제 시간이 존재하는 사용자 중 cutoff 이전(또는 같은 시각)에 삭제된 사용자만 조회한다.
            qUser.deletedAt.isNotNull(),
            qUser.deletedAt.loe(cutoff)
        )
        .fetch();
  }
}
