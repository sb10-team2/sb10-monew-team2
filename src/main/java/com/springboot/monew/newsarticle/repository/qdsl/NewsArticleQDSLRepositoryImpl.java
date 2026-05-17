package com.springboot.monew.newsarticle.repository.qdsl;

import static com.springboot.monew.comment.entity.QComment.comment;
import static com.springboot.monew.newsarticle.entity.QArticleInterest.articleInterest;
import static com.springboot.monew.newsarticle.entity.QArticleView.articleView;
import static com.springboot.monew.newsarticle.entity.QNewsArticle.newsArticle;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.newsarticle.dto.ParsedCursor;
import com.springboot.monew.newsarticle.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticle.dto.response.NewsArticleCursorRow;
import com.springboot.monew.newsarticle.entity.NewsArticle;
import com.springboot.monew.newsarticle.enums.NewsArticleDirection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NewsArticleQDSLRepositoryImpl implements NewsArticleQDSLRepository {

  private final JPAQueryFactory queryFactory;
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Override
  public List<NewsArticleCursorRow> findNewsArticles(NewsArticlePageRequest request, UUID userId) {
    /*
     * 1. news_articles 기준 조건 생성
     *
     * 목록 조회의 기준은 news_articles다.
     * comments, article_views를 먼저 JOIN하지 않고,
     * 기사 자체의 필터 조건만 먼저 적용한다.
     */
    BooleanBuilder where = new BooleanBuilder();

    where.and(newsArticle.isDeleted.isFalse());
    where.and(keywordContains(normalize(request.keyword())));
    where.and(sourceIn(request));
    where.and(publishDateGoe(request.publishDateFrom()));
    where.and(publishDateLoe(request.publishDateTo()));

    /*
     * 관심사 필터가 있는 경우에만 article_interests 조건을 적용한다.
     *
     * 기존처럼 항상 LEFT JOIN하면 관심사 필터가 없는 기본 목록 조회에서도
     * 불필요한 JOIN이 발생한다.
     *
     * EXISTS를 사용하면 article_interests로 인해 기사 row가 중복되지 않는다.
     */
    if (request.interestId() != null) {
      where.and(
          JPAExpressions
              .selectOne()
              .from(articleInterest)
              .where(
                  articleInterest.newsArticle.eq(newsArticle),
                  articleInterest.interest.id.eq(request.interestId())
              )
              .exists()
      );
    }

    /*
     * 커서 조건 적용
     *
     * publishedAt, viewCount처럼 news_articles 컬럼 기준 정렬이면
     * 여기서 커서 조건을 적용할 수 있다.
     *
     * commentCount는 집계값이라 이 단계에서 처리하지 않는다.
     */
    BooleanExpression cursorCondition = buildWhereCursorCondition(request);
    if (cursorCondition != null) {
      where.and(cursorCondition);
    }

    /*
     * 2. 기사 목록을 먼저 조회한다.
     *
     * 이 단계에서는 댓글 수, 조회 여부를 JOIN하지 않는다.
     * 먼저 화면에 보여줄 기사 limit + 1개만 확정한다.
     */
    List<NewsArticle> articles = queryFactory
        .selectFrom(newsArticle)
        .where(where)
        .orderBy(getArticleOrderSpecifiers(request).toArray(OrderSpecifier[]::new))
        .limit(request.limit() + 1L)
        .fetch();

    if (articles.isEmpty()) {
      return List.of();
    }

    /*
     * 3. 현재 페이지 기사 ID 추출
     *
     * 이후 부가 정보 조회는 전체 기사가 아니라
     * 현재 페이지에 포함된 기사만 대상으로 한다.
     */
    List<UUID> articleIds = articles.stream()
        .map(NewsArticle::getId)
        .toList();

    /*
     * 4. 현재 페이지 기사들의 댓글 수 조회
     *
     * 기존 구조에서는 comments를 목록 쿼리에 JOIN해서
     * 전체 결과에 GROUP BY가 걸렸다.
     *
     * 개선 구조에서는 이미 조회된 articleIds에 대해서만 댓글 수를 집계한다.
     */
    NumberExpression<Long> commentCountExpr = comment.id.count();
    Map<UUID, Long> commentCountMap = queryFactory
        .select(comment.article.id, commentCountExpr)
        .from(comment)
        .where(
            comment.article.id.in(articleIds),
            comment.isDeleted.isFalse()
        )
        .groupBy(comment.article.id)
        .fetch()
        .stream()
        .collect(Collectors.toMap(
            tuple -> tuple.get(comment.article.id),
            tuple -> tuple.get(commentCountExpr)
        ));

    /*
     * 5. 현재 사용자가 조회한 기사 ID 조회
     *
     * 기존 구조에서는 article_views를 목록 쿼리에 LEFT JOIN했다.
     * 개선 구조에서는 현재 페이지 articleIds 중 사용자가 조회한 것만 따로 가져온다.
     */
    Set<UUID> viewedArticleIds = new HashSet<>(
        queryFactory
            .select(articleView.newsArticle.id)
            .from(articleView)
            .where(
                articleView.user.id.eq(userId),
                articleView.newsArticle.id.in(articleIds)
            )
            .fetch()
    );

    /*
     * 6. 최종 DTO 조립
     *
     * DB에서는 기사 목록, 댓글 수, 조회 여부를 각각 작게 조회하고
     * Java에서 합친다.
     */
    return articles.stream()
        .map(article -> new NewsArticleCursorRow(
            article.getId(),
            article.getSource(),
            article.getOriginalLink(),
            article.getTitle(),
            article.getPublishedAt(),
            article.getSummary(),
            commentCountMap.getOrDefault(article.getId(), 0L),
            article.getViewCount(),
            viewedArticleIds.contains(article.getId()),
            article.getCreatedAt()
        ))
        .toList();
  }

  @Override
  public long countNewsArticles(NewsArticlePageRequest request) {
    Long count = queryFactory
        .select(newsArticle.id.count())
        .from(newsArticle)
        .where(buildBaseWhere(request))
        .fetchOne();

    return count == null ? 0L : count;
  }

  private BooleanBuilder buildBaseWhere(NewsArticlePageRequest request) {
    BooleanBuilder where = new BooleanBuilder();
    where.and(newsArticle.isDeleted.isFalse());
    where.and(keywordContains(normalize(request.keyword())));
    where.and(sourceIn(request));
    where.and(publishDateGoe(request.publishDateFrom()));
    where.and(publishDateLoe(request.publishDateTo()));

    if (request.interestId() != null) {
      where.and(
          JPAExpressions
              .selectOne()
              .from(articleInterest)
              .where(
                  articleInterest.newsArticle.eq(newsArticle),
                  articleInterest.interest.id.eq(request.interestId())
              )
              .exists()
      );
    }

    return where;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null) {
      return null;
    }

    return newsArticle.title.containsIgnoreCase(keyword)
        .or(newsArticle.summary.containsIgnoreCase(keyword));
  }

  private BooleanExpression sourceIn(NewsArticlePageRequest request) {
    if (request.sourceIn() == null || request.sourceIn().isEmpty()) {
      return null;
    }

    return newsArticle.source.in(request.sourceIn());
  }

  private BooleanExpression publishDateGoe(LocalDateTime publishDateFrom) {
    if (publishDateFrom == null) {
      return null;
    }
    return newsArticle.publishedAt.goe(
        publishDateFrom.atZone(KST).toInstant()
    );
  }

  private BooleanExpression publishDateLoe(LocalDateTime publishDateTo) {
    if (publishDateTo == null) {
      return null;
    }
    return newsArticle.publishedAt.loe(
        publishDateTo.atZone(KST).toInstant()
    );
  }

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

  private BooleanExpression publishDateCursorCondition(NewsArticlePageRequest request) {
    ParsedCursor parsedCursor = parseCursor(request.cursor());
    Instant cursorValue = parseInstantCursor(parsedCursor.value(), "cursor.value");

    BooleanExpression primaryCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.publishedAt.lt(cursorValue)
        : newsArticle.publishedAt.gt(cursorValue);

    if (parsedCursor.after() == null) {
      return primaryCondition;
    }

    BooleanExpression sameValueCondition = request.direction() == NewsArticleDirection.DESC
        ? newsArticle.createdAt.lt(parsedCursor.after())
        : newsArticle.createdAt.gt(parsedCursor.after());

    return primaryCondition.or(
        newsArticle.publishedAt.eq(cursorValue).and(sameValueCondition)
    );
  }

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

  private List<OrderSpecifier<?>> getArticleOrderSpecifiers(NewsArticlePageRequest request) {
    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
    Order direction = request.direction() == NewsArticleDirection.ASC ? Order.ASC : Order.DESC;

    switch (request.orderBy()) {
      case publishDate ->
          orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.publishedAt));
      case viewCount ->
          orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.viewCount));
      case commentCount -> {
      }
    }

    orderSpecifiers.add(new OrderSpecifier<>(direction, newsArticle.createdAt));

    return orderSpecifiers;
  }

  private ParsedCursor parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new ParsedCursor(null, null);
    }

    String[] parts = cursor.split("\\|");
    if (parts.length != 2) {
      throw new IllegalArgumentException("잘못된 커서 형식입니다. cursor|after");
    }

    String value = parts[0];
    if (value.isBlank()) {
      throw new IllegalArgumentException("cursor는 비어 있을 수 없습니다");
    }

    Instant after = parseInstantCursor(parts[1], "cursor.after");
    return new ParsedCursor(value, after);
  }

  private Instant parseInstantCursor(String raw, String field) {
    try {
      return Instant.parse(raw);
    } catch (Exception e) {
      throw new IllegalArgumentException("잘못된 커서 형식입니다. " + field);
    }
  }

  private Long parseLongCursor(String raw, String field) {
    try {
      return Long.parseLong(raw);
    } catch (Exception e) {
      throw new IllegalArgumentException("잘못된 커서 형식입니다. " + field);
    }
  }
}
