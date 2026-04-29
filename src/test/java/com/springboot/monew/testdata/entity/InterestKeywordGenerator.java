package com.springboot.monew.testdata.entity;

import static org.instancio.Select.field;

import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.Keyword;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Setter;
import org.instancio.Instancio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InterestKeywordGenerator extends BaseGenerator<InterestKeyword> {

  @Setter
  private int keywordPerInterest = 3;

  public InterestKeywordGenerator(JdbcTemplate template,
      @Qualifier("jdbcWorker") Executor executor) {
    super(template, executor);
  }

  public List<InterestKeyword> run(List<Interest> interests, List<Keyword> keywords) {
    AtomicInteger offset = new AtomicInteger(0);
    return generate(interests.size(), relationMappingGenerator(interests, offset,
        interest -> createKeywordsFor(interest, keywords)));
  }

  private Stream<InterestKeyword> createKeywordsFor(Interest interest, List<Keyword> keywords) {
    return uniqueRandomNumbers(keywords.size(), keywordPerInterest).stream()
        .map(idx -> Instancio.of(InterestKeyword.class)
            .generate(field(InterestKeyword::getCreatedAt), this::betweenNowAndTwoWeeksAgo)
            .set(field(InterestKeyword::getInterest), interest)
            .set(field(InterestKeyword::getKeyword), keywords.get(idx))
            .create());
  }

  @Override
  protected void setValues(PreparedStatement ps, InterestKeyword entity) throws SQLException {
    ps.setObject(1, entity.getId());
    ps.setObject(2, entity.getInterest().getId());
    ps.setObject(3, entity.getKeyword().getId());
    ps.setObject(4, Timestamp.from(entity.getCreatedAt()));
  }

  @Override
  protected String sql() {
    return insertSql("interest_keywords", "id", "interest_id", "keyword_id", "created_at");
  }

  @Override
  protected int batchSize() {
    if (super.batchSize() < keywordPerInterest) {
      throw new IllegalArgumentException(
          "keywordPerInterest=%s 는 1000을 넘길 수 없다".formatted(keywordPerInterest));
    }
    return Math.max(1, super.batchSize() / keywordPerInterest);
  }
}
