package com.springboot.datagenerator.generator;

import static org.instancio.Select.field;

import com.springboot.datagenerator.config.GeneratorProperties;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.Subscription;
import com.springboot.monew.user.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionGenerator extends BaseGenerator<Subscription> {

  public SubscriptionGenerator(GeneratorProperties properties,
      JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(properties, template, executor);
  }

  public List<Subscription> run(List<User> users, List<Interest> interests) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(users.size(), relationMappingGenerator(users, offset,
        user -> createInterestsFor(user, interests)));
  }

  private Stream<Subscription> createInterestsFor(User user, List<Interest> interests) {
    return uniqueRandomNumbers(interests.size(), properties.interestPerUser()).stream()
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
    if (super.batchSize() < properties.interestPerUser()) {
      throw new IllegalArgumentException(
          "interestPerUser=%s 는 1000을 넘길 수 없다".formatted(properties.interestPerUser()));
    }
    return Math.max(1, super.batchSize() / properties.interestPerUser());
  }
}
