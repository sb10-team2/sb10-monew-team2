package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Interest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InterestGenerator extends BaseGenerator<Interest> {

  public InterestGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<UUID> run(int size) {
    return generate(size, generator());
  }

  private Function<Integer, List<Interest>> generator() {
    Faker fake = faker.get();
    return chunkSize -> Instancio.ofList(Interest.class)
        .size(chunkSize)
        .supply(field(Interest::getName), () -> fake.lorem().characters(4, 50))
        .set(field(Interest::getSubscriberCount), 0L)
        .generate(field(Interest::getCreatedAt), gen -> gen.temporal().instant().range(twoWeeksAgo, now))
        .ignore(field(Interest::getUpdatedAt))
        .create();
  }

  @Override
  protected void setValues(PreparedStatement ps, Interest entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getName());
    ps.setObject(3, entity.getSubscriberCount());
    ps.setObject(4, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(5, entity.getUpdatedAt());
  }

  @Override
  protected String sql() {
    return "insert into interests (id, name, subscriber_count, created_at, updated_at)"
        + "values (?, ?, ?, ?, ?)";
  }
}
