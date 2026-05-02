package com.springboot.datagenerator.handler;

import com.springboot.datagenerator.config.TestDataProcessorProperties;
import com.springboot.datagenerator.constant.MonewApi;
import com.springboot.datagenerator.task.FetchTask;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseApiTestDataProcessor implements ApiTestDataProcessor {

  private final MonewApi domain;
  protected final JdbcTemplate template;
  protected final TestDataProcessorProperties properties;

  @Override
  public MonewApi getTargetApi() {
    return domain;
  }

  @Override
  public List<Map<String, Object>> fetch(FetchTask task) {
    boolean isFirstPage = task.cursor() == null || task.lastId() == null;
    String sql = getSql(isFirstPage);
    String prefix = task.prefix() + "%";
    if (isFirstPage) {
      return template.queryForList(sql, prefix, properties.batchSize());
    }
    return template.queryForList(sql, prefix, task.cursor(), task.lastId(), properties.batchSize());
  }

  @Override
  public String transform(List<Map<String, Object>> chunk) {
    StringBuilder sb = new StringBuilder();
    for (Map<String, Object> row : chunk) {
      StringJoiner joiner = new StringJoiner(",");
      for (String col : getColumns()) {
        Object value = row.getOrDefault(col, "");
        joiner.add(value.toString());
      }
      sb.append(joiner).append("\n");
    }
    return sb.toString();
  }

  protected abstract String getSql(boolean isFirstPage);

  protected abstract List<String> getColumns();
}
