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
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.newsarticles.dto.ParsedCursor;
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
    BooleanExpression whereCursorCondition = buildWhereCursorCondition(request);
    if (whereCursorCondition != null) {
      where.and(whereCursorCondition);
    }

    BooleanExpression havingCursorCondition = buildHavingCursorCondition(request, commentCountExpr);

    JPAQuery<NewsArticleCursorRow> query = queryFactory
        .select(constructor(
            NewsArticleCursorRow.class,
            newsArticle.id,
            newsArticle.source,
            newsArticle.originalLink,
            newsArticle.title,
            newsArticle.publishedAt,
            newsArticle.summary,
            commentCountExpr,
            newsArticle.viewCount,
            articleView.id.isNotNull(),
            newsArticle.createdAt
        ))
        .from(newsArticle)
        .leftJoin(articleInterest).on(articleInterest.newsArticle.eq(newsArticle))
        .leftJoin(comment).on(
            comment.article.eq(newsArticle)
                .and(comment.isDeleted.isFalse())
        )
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
        );

    if (havingCursorCondition != null) {
      query.having(havingCursorCondition);
    }

    return query
        .orderBy(getOrderSpecifiers(request, commentCountExpr).toArray(OrderSpecifier[]::new))
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
  // publishDate 커서조건 -> where절
  // viewCount 커서조건 -> where절
  private BooleanExpression buildWhereCursorCondition(NewsArticlePageRequest request) {
    if (request.cursor() == null || request.cursor().isBlank()) {
      return null;
    }

    return switch (request.orderBy()) {
      case publishDate -> publishDateCursorCondition(request);
      case viewCount -> viewCountCursorCondition(request);
      case commentCount -> null;
    };
  }

  // commentCount 커서조건 -> having절
  // 집계함수라서 having절에 써야한다.
  private BooleanExpression buildHavingCursorCondition(
      NewsArticlePageRequest request,
      NumberExpression<Long> commentCountExpr
  ) {
    if (request.cursor() == null || request.cursor().isBlank()) {
      return null;
    }

    return switch (request.orderBy()) {
      case commentCount -> commentCountCursorCondition(request, commentCountExpr);
      case publishDate, viewCount -> null;
    };
  }

  // 날짜 정렬 기준 커서 조건
  //ORDER BY published_at DESC, created_at DESC
  private BooleanExpression publishDateCursorCondition(NewsArticlePageRequest request) {

    //parsedCursor = (value(주커서), after(보조커서)) 형태로 있다.
    ParsedCursor parsedCursor = parseCursor(request.cursor());

    //주커서
    //cursor값을 Instant로 파싱하는 과정에서 깨질때 대비한 예외처리
    Instant cursorValue = parseInstantCursor(parsedCursor.value(), "cursor.value");;

    //마지막 row의 publishedAt이 2026.04.24라면 2026.04.24 > publishedAt
    BooleanExpression primaryCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.publishedAt.lt(cursorValue)
        : newsArticle.publishedAt.gt(cursorValue);

    if (parsedCursor.after() == null) {
      return primaryCondition;
    }

    //주커서 값이 값은경우 createdAt으로 비교
    BooleanExpression sameValueCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.createdAt.lt(parsedCursor.after())
        : newsArticle.createdAt.gt(parsedCursor.after());

    return primaryCondition.or(
        newsArticle.publishedAt.eq(cursorValue).and(sameValueCondition)
    );
  }


  // 조회수 정렬 기준 커서 조건
  // after가 null로 들어와서 cursor로만 조회
  private BooleanExpression viewCountCursorCondition(NewsArticlePageRequest request) {
    ParsedCursor parsedCursor = parseCursor(request.cursor());

    Long cursorValue = parseLongCursor(parsedCursor.value(), "cursor.value");

    BooleanExpression primaryCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.viewCount.lt(cursorValue)
        : newsArticle.viewCount.gt(cursorValue);

    if (parsedCursor.after() == null) {
      return primaryCondition;
    }

    BooleanExpression sameValueCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.createdAt.lt(parsedCursor.after())
        : newsArticle.createdAt.gt(parsedCursor.after());

    return primaryCondition.or(
        newsArticle.viewCount.eq(cursorValue).and(sameValueCondition)
    );
  }

  // 댓글 수 정렬 기준 커서 조건
  // 집계값(count)이므로 DB/JPA 조합에 따라 having으로 분리해야 할 수도 있음
  private BooleanExpression commentCountCursorCondition(
      NewsArticlePageRequest request,
      NumberExpression<Long> commentCountExpr
  ) {
    ParsedCursor parsedCursor = parseCursor(request.cursor());

    Long cursorValue = parseLongCursor(parsedCursor.value(), "cursor.value");

    BooleanExpression primaryCondition = request.direction() == NewsArticleDirection.DESC
        ? commentCountExpr.lt(cursorValue)
        : commentCountExpr.gt(cursorValue);

    if (parsedCursor.after() == null) {
      return primaryCondition;
    }

    BooleanExpression sameValueCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.createdAt.lt(parsedCursor.after())
        : newsArticle.createdAt.gt(parsedCursor.after());

    return primaryCondition.or(
        commentCountExpr.eq(cursorValue).and(sameValueCondition)
    );
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

  private ParsedCursor parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new ParsedCursor(null, null);
    }
    // 1. |가 아예 없는 경우 통과
    // 2. |가 여러개 있는 경우 통과
    // 3. cursor = "10|" 인 경우 통과
    // 4. cursor = "|2026-04-24T10:00:00Z"인 경우 통과
    // ToDo: 위 4가지 경우를 막아야한다. -> 아래 if절로 3번까지 방어
    String[] parts = cursor.split("\\|");

    //[value(cursor), after(보조 cursor)]형태가 아니면 예외처리
    if(parts.length != 2) {
      throw new IllegalArgumentException("잘못된 커서 형식입니다: cursor|after");
    }

    String value = parts[0];
    if (value.isBlank()) {
      throw new IllegalArgumentException("cursor는 비어 있을 수 없습니다");
    }

    Instant after = parseInstantCursor(parts[1], "cursor.after");

    return new ParsedCursor(value, after);
  }

  //cursor값을 Instant로 파싱하는 과정에서 깨질때 대비한 예외처리
  private Instant parseInstantCursor(String raw, String field){
    try {
      return Instant.parse(raw);
    }catch (Exception e){
      throw new IllegalArgumentException("잘못된 커서 형식입니다." + field);
    }
  }

  //cursor값을 Long으로 파싱하는 과정에서 깨질때 대비한 예외처리
  private Long parseLongCursor(String raw, String field) {
    try {
      return Long.parseLong(raw);
    } catch (Exception e) {
      throw new IllegalArgumentException("잘못된 커서 형식입니다: " + field);
    }
  }

}
