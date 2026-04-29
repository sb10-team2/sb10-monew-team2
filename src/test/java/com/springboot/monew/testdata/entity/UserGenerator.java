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

  public List<User> run(int size) {
    return generate(size, generator());
  }

  private Function<Integer, List<User>> generator() {
    Faker fake = faker.get();
    return chunkSize -> Instancio.ofList(User.class)
        .size(chunkSize)
        .supply(field(User::getEmail), () -> email(fake))
        .generate(field(User::getNickname),
            gen -> gen.text().pattern("user_#a#d#a#d#a#d#a#d#a#d#a#d#a#d#a"))
        .supply(field(User::getPassword), () -> fake.credentials().password(6, 255))
        .generate(field(User::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
        .ignore(field(User::getDeletedAt))
        .ignore(field(User::getUpdatedAt))
        .create();
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

  private String email(Faker fake) {
    return "%s_%s".formatted(UUID.randomUUID(), fake.internet().emailAddress());
  }


}
