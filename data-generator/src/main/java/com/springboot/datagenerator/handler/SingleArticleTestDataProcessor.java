package com.springboot.datagenerator.handler;

import com.springboot.datagenerator.config.TestDataProcessorProperties;
import com.springboot.datagenerator.constant.MonewApi;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SingleArticleTestDataProcessor extends BaseApiTestDataProcessor {

  public SingleArticleTestDataProcessor(JdbcTemplate template,
      TestDataProcessorProperties properties) {
    super(MonewApi.GET_ARTICLE, template, properties);
  }

  @Override
  protected String getSql(boolean isFirstPage) {
    return "";
  }

  @Override
  protected List<String> getColumns() {
    return List.of();
  }
}
