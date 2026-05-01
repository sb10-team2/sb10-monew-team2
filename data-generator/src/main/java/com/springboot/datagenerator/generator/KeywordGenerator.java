package com.springboot.datagenerator.generator;

import static org.instancio.Select.field;

import com.springboot.datagenerator.config.GeneratorProperties;
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

  public KeywordGenerator(GeneratorProperties properties,
      JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(properties, template, executor);
  }

  public List<Keyword> run() {
    Model<Keyword> model = Instancio.of(Keyword.class)
        .supply(field(Keyword::getName), () -> faker.get().lorem().characters(4, 100))
        .generate(field(Keyword::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
        .toModel();
    return generate(properties.keyword(), modelGenerator(model));
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
