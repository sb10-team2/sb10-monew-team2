package com.springboot.monew.newsarticles.repository.qdsl;

import static com.querydsl.core.types.Projections.constructor;
import static com.springboot.monew.comment.entity.QComment.comment;
import static com.springboot.monew.newsarticles.entity.QArticleInterest.articleInterest;
import static com.springboot.monew.newsarticles.entity.QArticleView.articleView;
import static com.springboot.monew.newsarticles.entity.QNewsArticle.newsArticle;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticles.enums.NewsArticleDirection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsArticleQDSLRepositoryImpl implements NewsArticleQDSLRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<NewsArticleCursorRow> findNewsArticles(NewsArticlePageRequest request, UUID userId) {

    // 동적 조건 조합을 위한 where 절
    BooleanBuilder where = new BooleanBuilder();

    // 논리삭제 되지 않은 기사만 조회
    where.and(newsArticle.isDeleted.isFalse());

    // 검색 키워드 조건 (제목 또는 요약 부분일치)
    where.and(keywordContains(normalize(request.keyword())));

    // 관심사 ID가 있으면 해당 관심사와 연결된 기사만 조회
    where.and(interestIdEq(request.interestId()));

    // 출처 필터가 있으면 해당 출처에 포함되는 기사만 조회
    where.and(sourceIn(request));

    // 날짜 범위 필터
    where.and(publishDateGoe(request.publishDateFrom()));
    where.and(publishDateLoe(request.publishDateTo()));

    // 댓글 수 정렬/조회에 사용할 댓글 개수 집계 표현식
    // 댓글 ROW 중복 방지를 위해 countDistinct 사용
    NumberExpression<Long> commentCountExpr = comment.id.countDistinct();

    // cursor, after 값이 있으면 다음 페이지 조건 생성
    // 정렬 기준(orderBy)에 따라 날짜/댓글수/조회수 기준 커서 조건이 달라진다.
    BooleanExpression cursorCondition = buildCursorCondition(request, commentCountExpr);
    if (cursorCondition != null) {
      where.and(cursorCondition);
    }

    return queryFactory
        .select(constructor(
            NewsArticleCursorRow.class,
            newsArticle.id,
            newsArticle.source,
            newsArticle.originalLink,   // sourceUrl
            newsArticle.title,
            newsArticle.publishedAt,    // publishDate
            newsArticle.summary,
            commentCountExpr,
            newsArticle.viewCount,
            articleView.id.isNotNull(), // viewedByMe
            newsArticle.createdAt       // nextAfter 계산용 내부 필드
        ))
        .from(newsArticle)
        // 관심사 필터용 조인
        .leftJoin(articleInterest).on(articleInterest.newsArticle.eq(newsArticle))
        // 댓글 수 집계용 조인
        .leftJoin(comment).on(
            comment.article.eq(newsArticle)
                .and(comment.isDeleted.isFalse())
        )
        // 현재 유저가 조회했는지 확인용 조인
        .leftJoin(articleView).on(
            articleView.newsArticle.eq(newsArticle)
                .and(articleView.user.id.eq(userId))
        )
        .where(where)
        .groupBy(
            newsArticle.id,
            newsArticle.source,
            newsArticle.originalLink,
            newsArticle.title,
            newsArticle.publishedAt,
            newsArticle.summary,
            newsArticle.viewCount,
            articleView.id,
            newsArticle.createdAt
        )
        .orderBy(getOrderSpecifiers(request, commentCountExpr).toArray(OrderSpecifier[]::new))
        // 다음 페이지 존재 여부 확인 위해 limit + 1 조회
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public long countNewsArticles(NewsArticlePageRequest request) {

    BooleanBuilder where = new BooleanBuilder();

    // 삭제되지 않은 기사만 count
    where.and(newsArticle.isDeleted.isFalse());

    // 검색/필터 조건 동일 적용
    where.and(keywordContains(normalize(request.keyword())));
    where.and(interestIdEq(request.interestId()));
    where.and(sourceIn(request));
    where.and(publishDateGoe(request.publishDateFrom()));
    where.and(publishDateLoe(request.publishDateTo()));

    Long count = queryFactory
        .select(newsArticle.id.countDistinct())
        .from(newsArticle)
        .leftJoin(articleInterest).on(articleInterest.newsArticle.eq(newsArticle))
        .where(where)
        .fetchOne();

    return count == null ? 0L : count;
  }

  // 공백 제거 후 빈 값이면 null 반환
  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  // 제목 또는 요약에 검색어가 부분일치하는 조건
  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null) {
      return null;
    }

    return newsArticle.title.containsIgnoreCase(keyword)
        .or(newsArticle.summary.containsIgnoreCase(keyword));
  }

  // 관심사 조건
  private BooleanExpression interestIdEq(UUID interestId) {
    if (interestId == null) {
      return null;
    }

    return articleInterest.interest.id.eq(interestId);
  }

  // 출처 포함 조건
  private BooleanExpression sourceIn(NewsArticlePageRequest request) {
    if (request.sourceIn() == null || request.sourceIn().isEmpty()) {
      return null;
    }

    return newsArticle.source.in(request.sourceIn());
  }

  // 날짜 시작 범위 조건
  private BooleanExpression publishDateGoe(Instant publishDateFrom) {
    if (publishDateFrom == null) {
      return null;
    }

    return newsArticle.publishedAt.goe(publishDateFrom);
  }

  // 날짜 끝 범위 조건
  private BooleanExpression publishDateLoe(Instant publishDateTo) {
    if (publishDateTo == null) {
      return null;
    }

    return newsArticle.publishedAt.loe(publishDateTo);
  }

  // 정렬 기준에 따른 커서 조건 생성
  private BooleanExpression buildCursorCondition(
      NewsArticlePageRequest request,
      NumberExpression<Long> commentCountExpr
  ) {
    // cursor 자체가 없으면 커서 페이지네이션 조건을 적용할 수 없음
    if (request.cursor() == null || request.cursor().isBlank()) {
      return null;
    }

    return switch (request.orderBy()) {
      case publishDate -> publishDateCursorCondition(request);
      case commentCount -> commentCountCursorCondition(request, commentCountExpr);
      case viewCount -> viewCountCursorCondition(request);
    };
  }

  // 날짜 정렬 기준 커서 조건
  private BooleanExpression publishDateCursorCondition(NewsArticlePageRequest request) {
    Instant cursorValue = Instant.parse(request.cursor());

    // after가 없으면 publishDate만으로 다음 페이지 조건 생성
    if (request.after() == null || request.after().isBlank()) {
      return request.direction() == NewsArticleDirection.DESC
          ? newsArticle.publishedAt.lt(cursorValue)
          : newsArticle.publishedAt.gt(cursorValue);
    }

    Instant afterValue = Instant.parse(request.after());

    if (request.direction() == NewsArticleDirection.DESC) {
      return newsArticle.publishedAt.lt(cursorValue)
          .or(newsArticle.publishedAt.eq(cursorValue)
              .and(newsArticle.createdAt.lt(afterValue)));
    }

    return newsArticle.publishedAt.gt(cursorValue)
        .or(newsArticle.publishedAt.eq(cursorValue)
            .and(newsArticle.createdAt.gt(afterValue)));
  }


  // 조회수 정렬 기준 커서 조건
  private BooleanExpression viewCountCursorCondition(NewsArticlePageRequest request) {
    Long cursorValue = Long.parseLong(request.cursor());

    // after가 없으면 viewCount만으로 다음 페이지 조건 생성
    if (request.after() == null || request.after().isBlank()) {
      return request.direction() == NewsArticleDirection.DESC
          ? newsArticle.viewCount.lt(cursorValue)
          : newsArticle.viewCount.gt(cursorValue);
    }

    Instant afterValue = Instant.parse(request.after());

    if (request.direction() == NewsArticleDirection.DESC) {
      return newsArticle.viewCount.lt(cursorValue)
          .or(newsArticle.viewCount.eq(cursorValue)
              .and(newsArticle.createdAt.lt(afterValue)));
    }

    return newsArticle.viewCount.gt(cursorValue)
        .or(newsArticle.viewCount.eq(cursorValue)
            .and(newsArticle.createdAt.gt(afterValue)));
  }

  // 댓글 수 정렬 기준 커서 조건
  // 집계값(count)이므로 DB/JPA 조합에 따라 having으로 분리해야 할 수도 있음
  private BooleanExpression commentCountCursorCondition(
      NewsArticlePageRequest request,
      NumberExpression<Long> commentCountExpr
  ) {
    Long cursorValue = Long.parseLong(request.cursor());

    // after가 없으면 commentCount만으로 다음 페이지 조건 생성
    if (request.after() == null || request.after().isBlank()) {
      return request.direction() == NewsArticleDirection.DESC
          ? commentCountExpr.lt(cursorValue)
          : commentCountExpr.gt(cursorValue);
    }

    Instant afterValue = Instant.parse(request.after());

    if (request.direction() == NewsArticleDirection.DESC) {
      return commentCountExpr.lt(cursorValue)
          .or(commentCountExpr.eq(cursorValue)
              .and(newsArticle.createdAt.lt(afterValue)));
    }

    return commentCountExpr.gt(cursorValue)
        .or(commentCountExpr.eq(cursorValue)
            .and(newsArticle.createdAt.gt(afterValue)));
  }

  // 정렬 기준 + 보조 정렬(createdAt) 생성
  private List<OrderSpecifier<?>> getOrderSpecifiers(
      NewsArticlePageRequest request,
      NumberExpression<Long> commentCountExpr
  ) {
    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
    Order direction = request.direction() == NewsArticleDirection.ASC ? Order.ASC : Order.DESC;

    switch (request.orderBy()) {
      case publishDate ->
          orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.publishedAt));
      case commentCount ->
          orderSpecifiers.add(new OrderSpecifier<>(direction, commentCountExpr));
      case viewCount ->
          orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.viewCount));
    }

    // 같은 정렬값이 있을 때 페이지 경계를 안정적으로 자르기 위한 보조 정렬
    orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.createdAt));

    return orderSpecifiers;
  }


}
