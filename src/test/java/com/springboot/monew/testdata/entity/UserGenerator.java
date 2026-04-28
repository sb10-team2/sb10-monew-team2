package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.users.entity.User;
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
public class UserGenerator extends BaseGenerator<User> {

  public UserGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<UUID> run(int size) {
    return generate(size, generator());
  }

  private Function<Integer, List<User>> generator() {
    return chunkSize -> {
      Faker fake = faker.get();
      return Instancio.ofList(User.class)
          .size(chunkSize)
          .supply(field(User::getEmail), () -> fake.internet().emailAddress())
//          .supply(field(User::getNickname), () -> fake.funnyName().name())
          .supply(field(User::getPassword), () -> fake.credentials().password(6, 255))
          .supply(field(User::getCreatedAt), this::timestamp)
          .ignore(field(User::getDeletedAt))
          .ignore(field(User::getUpdatedAt))
          .create();
    };
  }

  @Override
  protected void setValues(PreparedStatement ps, User entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getEmail());
    ps.setObject(3, entity.getNickname());
    ps.setObject(4, entity.getPassword());
    ps.setObject(5, entity.getDeletedAt());
    ps.setObject(6, Timestamp.from(entity.getCreatedAt()));
    ps.setObject(7, entity.getUpdatedAt());
  }

  @Override
  protected String sql() {
    return "insert into users (id, email, nickname, password, deleted_at, created_at, updated_at) "
        + "values (?, ?, ?, ?, ?, ?, ?)";
  }
}
