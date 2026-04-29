package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Keyword;
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
public class KeywordGenerator extends BaseGenerator<Keyword> {

  public KeywordGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Keyword> run(int size) {
    return generate(size, generator());
  }

  private Function<Integer, List<Keyword>> generator() {
    Faker fake = faker.get();
    return chunkSize -> Instancio.ofList(Keyword.class)
        .size(chunkSize)
        .supply(field(Keyword::getName), () -> fake.lorem().characters(4, 100))
        .generate(field(Keyword::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
        .create();
  }

  @Override
  protected void setValues(PreparedStatement ps, Keyword entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getName());
    ps.setObject(3, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return "insert into keywords (id, name, created_at) values (?, ?, ?)";
  }
}
