package com.springboot.datagenerator.handler;

import com.springboot.datagenerator.constant.MonewDomain;
import com.springboot.datagenerator.task.FetchTask;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseFetchHandler implements DomainFetchHandler {

  private final MonewDomain domain;
  protected final JdbcTemplate template;

  @Override
  public boolean matchDomain(MonewDomain domain) {
    return this.domain == domain;
  }

  @Override
  public List<Map<String, Object>> fetch(FetchTask task) {
    return template.queryForList(getSql(), task.prefix() + "%", task.cursor(), task.lastId());
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

  protected abstract String getSql();

  protected abstract List<String> getColumns();
}
