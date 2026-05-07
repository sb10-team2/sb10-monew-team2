package com.springboot.monew.interest.repository.qdsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestDirection;
import com.springboot.monew.interest.entity.InterestOrderBy;
import com.springboot.monew.interest.entity.QInterest;
import com.springboot.monew.interest.entity.QInterestKeyword;
import com.springboot.monew.interest.entity.QKeyword;
import com.springboot.monew.newsarticle.entity.QArticleInterest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
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
  private final QInterestKeyword qInterestKeyword = QInterestKeyword.interestKeyword;
  private final QKeyword qKeyword = QKeyword.keyword;
  private final QArticleInterest qArticleInterest = QArticleInterest.articleInterest;

  @Override
  public Optional<Interest> findByIdWithArticleCount(UUID id) {
    Tuple result = queryFactory.select(qInterest, qArticleInterest.count())
        .from(qInterest)
        .leftJoin(qArticleInterest).on(qArticleInterest.interest.eq(qInterest))
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

  @Override
  public List<Interest> findInterests(InterestPageRequest request) {
    String normalizedKeyword = normalize(request.keyword());

    BooleanBuilder where = new BooleanBuilder();
    where.and(keywordContains(normalizedKeyword));
    where.and(cursorCondition(request));

    JPAQuery<Interest> query = normalizedKeyword == null
        ? queryFactory.selectFrom(qInterest)
        : interestSearchQuery();

    return query
        .where(where)
        .orderBy(orderSpecifiers(request.orderBy(), request.direction()))
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public long countInterests(String keyword) {
    String normalizedKeyword = normalize(keyword);

    if (normalizedKeyword == null) {
      Long count = queryFactory.select(qInterest.id.count())
          .from(qInterest)
          .fetchOne();

      return count == null ? 0L : count;
    }

    Long count = queryFactory.select(qInterest.id.countDistinct())
        .from(qInterest)
        .leftJoin(qInterestKeyword).on(qInterestKeyword.interest.eq(qInterest))
        .leftJoin(qInterestKeyword.keyword, qKeyword)
        .where(keywordContains(normalizedKeyword))
        .fetchOne();

    return count == null ? 0L : count;
  }

  // 관심사 목록 검색에 공통으로 사용하는 기본 쿼리를 생성
  private JPAQuery<Interest> interestSearchQuery() {
    // 관심사와 키워드를 조인하고 중복 관심사를 제거하는 쿼리 생성
    return queryFactory.selectDistinct(qInterest)
        .from(qInterest)
        .leftJoin(qInterestKeyword).on(qInterestKeyword.interest.eq(qInterest))
        .leftJoin(qInterestKeyword.keyword, qKeyword);
  }

  // 관심사 이름이나 키워드 이름에 검색어가 포함되는 조건을 생성
  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null) {
      return null;
    }

    // 대소문자 구분 없이 구분하기 위해 검색어를 소문자로 변환
    // 관심사 등록 시 대소문자를 구분해서 유사도 검사를 하는데 검색에선 대소문자 구분 없이 검색하는 이유
    // -> 사용자는 관심사의 정책을 알 수 없고, 가능한 많은 후보를 노출해 사용자가 원하는 관심사를 직접 선택하도록
    //    해야 한다. 즉, 검색은 검증이 아니라 탐색에 초점을 두기 때문에 등록과 다르게 대소문자를 구분하지 않는다.
    String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
    // 관심사 이름 또는 키워드 이름에 검색어가 포함되는 조건을 반환
    return qInterest.name.lower().contains(lowerKeyword)
        .or(qKeyword.name.lower().contains(lowerKeyword));
  }

  // 요청된 정렬 기준과 커서 값에 맞는 페이지네이션 조건을 생성
  private BooleanExpression cursorCondition(InterestPageRequest request) {
    // 요청값에서 커서와 after 값을 추출
    CursorValue cursorValue = CursorValue.from(request);
    if (cursorValue.value() == null) {
      return null;
    }

    // 이름 정렬이면 이름 기준 커서 조건을 생성
    if (request.orderBy() == InterestOrderBy.name) {
      return nameCursorCondition(cursorValue.value(), cursorValue.after(), request.direction());
    }

    // 구독자 수 커서를 long 값으로 변환
    long subscriberCountCursor = parseSubscriberCountCursor(cursorValue.value());
    // 구독자 수 기준 커서 조건을 생성
    return subscriberCountCursorCondition(
        subscriberCountCursor,
        cursorValue.after(),
        request.direction()
    );
  }

  // 이름 정렬 시 현재 커서 이후 또는 이전 데이터를 조회하기 위한 조건을 생성
  private BooleanExpression nameCursorCondition(String cursor, Instant after,
      InterestDirection direction) {
    // 정렬 방향에 따라 이름의 대소 비교 조건을 생성
    BooleanExpression primaryCondition = direction == InterestDirection.ASC
        ? qInterest.name.gt(cursor)
        : qInterest.name.lt(cursor);

    // after 값이 없으면 이름 비교 조건만 반환
    if (after == null) {
      return primaryCondition;
    }

    // 이름이 같은 경우 생성 시각으로 순서를 이어가기 위한 조건을 생성
    BooleanExpression sameNameCondition = direction == InterestDirection.ASC
        ? qInterest.createdAt.gt(after)
        : qInterest.createdAt.lt(after);

    // 이름 비교 조건과 동일 이름일 때의 생성 시각 조건을 합쳐 반환
    return primaryCondition.or(qInterest.name.eq(cursor).and(sameNameCondition));
  }

  // 구독자 수 정렬 시 현재 커서 이후 또는 이전 데이터를 조회하기 위한 조건을 생성
  private BooleanExpression subscriberCountCursorCondition(long cursor, Instant after,
      InterestDirection direction) {
    // 정렬 방향에 따라 구독자 수의 대소 비교 조건을 생성
    BooleanExpression primaryCondition = direction == InterestDirection.ASC
        ? qInterest.subscriberCount.gt(cursor)
        : qInterest.subscriberCount.lt(cursor);

    // after 값이 없으면 구독자 수 비교 조건만 반환
    if (after == null) {
      return primaryCondition;
    }

    // 구독자 수가 같은 경우 생성 시각으로 순서를 이어가기 위한 조건을 생성
    BooleanExpression sameCountCondition = direction == InterestDirection.ASC
        ? qInterest.createdAt.gt(after)
        : qInterest.createdAt.lt(after);

    // 구독자 수 비교 조건과 동일 구독자 수일 때의 생성 시각 조건을 합쳐 반환
    return primaryCondition.or(qInterest.subscriberCount.eq(cursor).and(sameCountCondition));
  }

  // 요청된 정렬 기준과 방향에 맞는 정렬 조건 배열을 생성
  private OrderSpecifier<?>[] orderSpecifiers(InterestOrderBy orderBy,
      InterestDirection direction) {
    // 정렬 방향을 Order 타입으로 변환
    Order order = direction == InterestDirection.ASC ? Order.ASC : Order.DESC;

    // 요청된 정렬 기준에 따라 정렬 조건 생성
    OrderSpecifier<?> primaryOrder = orderBy == InterestOrderBy.name
        ? new OrderSpecifier<>(order, qInterest.name)
        : new OrderSpecifier<>(order, qInterest.subscriberCount);

    // createdAt을 보조 정렬 기준으로 추가
    return new OrderSpecifier<?>[]{
        primaryOrder,
        new OrderSpecifier<>(order, qInterest.createdAt)
    };
  }

  private long parseSubscriberCountCursor(String cursor) {
    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid subscriberCount cursor: " + cursor, e);
    }
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  // 요청에서 추출한 커서 값과 after 값을 함께 보관하는 레코드
  private record CursorValue(String value, Instant after) {

    // 요청 객체에서 커서 값과 after 값을 꺼내 CursorValue로 변환
    private static CursorValue from(InterestPageRequest request) {
      String cursor = normalize(request.cursor());

      if (cursor == null) {
        return new CursorValue(null, request.after());
      }

      // 정렬값이 구독자 수일 경우 커서에서 구독자 수와 생성 시각 분리
      if (request.orderBy() == InterestOrderBy.subscriberCount && cursor.contains("|")) {
        String[] parts = cursor.split("\\|", 2);

        try {
          return new CursorValue(parts[0], Instant.parse(parts[1]));
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException("Invalid subscriberCount cursor: " + cursor, e);
        }
      }

      return new CursorValue(cursor, request.after());
    }

    private static String normalize(String value) {
      if (value == null || value.isBlank()) {
        return null;
      }
      return value.trim();
    }
  }
}
