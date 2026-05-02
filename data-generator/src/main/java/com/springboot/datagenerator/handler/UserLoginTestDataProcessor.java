package com.springboot.datagenerator.handler;

import com.springboot.datagenerator.config.TestDataProcessorProperties;
import com.springboot.datagenerator.constant.MonewApi;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserLoginTestDataProcessor extends BaseApiTestDataProcessor {

  public UserLoginTestDataProcessor(JdbcTemplate template,
      TestDataProcessorProperties properties) {
    super(MonewApi.POST_USER_LOGIN, template, properties);
  }

  @Override
  protected String getSql(boolean isFirstPage) {
    StringBuilder sb = new StringBuilder();
    sb.append("select id, email, password, created_at from users");
    if (isFirstPage) {
      sb.append("where id::text like ? and (created_at, id::text) > (?, ?) ");
    }
    sb.append("order by created_at asc, id::text asc limit ?");
    return sb.toString();
  }

  @Override
  protected List<String> getColumns() {
    return List.of("email", "password");
  }
}
