package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Interest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InterestGenerator extends BaseGenerator<Interest> {

  public InterestGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Interest> run(int size) {
    Model<Interest> model = Instancio.of(Interest.class)
        .supply(field(Interest::getName), () -> faker.get().lorem().characters(4, 50))
        .set(field(Interest::getSubscriberCount), 0L)
        .generate(field(Interest::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
        .ignore(field(Interest::getUpdatedAt))
        .toModel();
    return generate(size, modelGenerator(model));
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
    return insertSql("interests", "id", "name", "subscriber_count", "created_at", "updated_at");
  }
}
