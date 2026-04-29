package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.users.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Setter;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionGenerator extends BaseGenerator<Subscription> {

  @Setter
  private int interestPerUser = 3;

  public SubscriptionGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Subscription> run(List<User> users, List<Interest> interests) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(users.size(), relationMappingGenerator(users, offset,
        user -> createInterestsFor(user, interests)));
  }

  private Stream<Subscription> createInterestsFor(User user, List<Interest> interests) {
    return uniqueRandomNumbers(interests.size(), interestPerUser).stream()
        .map(idx -> Instancio.of(Subscription.class)
            .generate(field(Subscription::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(Subscription::getUser), user)
            .set(field(Subscription::getInterest), interests.get(idx))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, Subscription entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getUser().getId());
    ps.setObject(3, entity.getInterest().getId());
    ps.setObject(4, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return insertSql("subscriptions", "id", "user_id", "interest_id", "created_at");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < interestPerUser) {
      throw new IllegalArgumentException(
          "interestPerUser=%s 는 1000을 넘길 수 없다".formatted(interestPerUser));
    }
    return Math.max(1, super.batchSize() / interestPerUser);
  }
}
