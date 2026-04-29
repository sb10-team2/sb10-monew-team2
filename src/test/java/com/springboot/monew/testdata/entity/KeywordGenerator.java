package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Keyword;
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
public class KeywordGenerator extends BaseGenerator<Keyword> {

  public KeywordGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<Keyword> run(int size) {
    Model<Keyword> model = Instancio.of(Keyword.class)
        .supply(field(Keyword::getName), () -> faker.get().lorem().characters(4, 100))
        .generate(field(Keyword::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
        .toModel();
    return generate(size, modelGenerator(model));
  }

  @Override
  protected void setValues(PreparedStatement ps, Keyword entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getName());
    ps.setObject(3, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return insertSql("keywords", "id", "name", "created_at");
  }
}
