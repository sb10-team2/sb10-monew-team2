package com.springboot.monew.interest.repository.qdsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.QInterest;
import com.springboot.monew.newsarticles.entity.QArticleInterest;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterestQDSLRepositoryImpl implements InterestQDSLRepository {
  private final JPAQueryFactory queryFactory;
  private final QInterest qInterest = QInterest.interest;
  private final QArticleInterest qArticleInterest = QArticleInterest.articleInterest;

  @Override
  public Optional<Interest> findByIdWithArticleCount(UUID id) {
    Tuple result = queryFactory.select(qInterest, qArticleInterest.count())
        .from(qInterest)
        .leftJoin(qInterest, qArticleInterest.interest)
        .where(qInterest.id.eq(id))
        .groupBy(qInterest)
        .fetchOne();

    if (result == null) {
      return Optional.empty();
    }
    Interest interest = Objects.requireNonNull(result.get(qInterest));
    Long articleCount = Objects.requireNonNull(result.get(qArticleInterest.count()));
    interest.setArticleCount(articleCount);
    return Optional.of(interest);
  }
}
