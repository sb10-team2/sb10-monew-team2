package com.springboot.monew.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.springboot.monew.interest.dto.request.InterestPageRequest;
import com.springboot.monew.interest.dto.request.InterestRegisterRequest;
import com.springboot.monew.interest.dto.request.InterestUpdateRequest;
import com.springboot.monew.interest.dto.response.CursorPageResponseInterestDto;
import com.springboot.monew.interest.dto.response.InterestDto;
import com.springboot.monew.interest.entity.Interest;
import com.springboot.monew.interest.entity.InterestDirection;
import com.springboot.monew.interest.entity.InterestKeyword;
import com.springboot.monew.interest.entity.InterestOrderBy;
import com.springboot.monew.interest.entity.Keyword;
import com.springboot.monew.interest.exception.InterestErrorCode;
import com.springboot.monew.interest.exception.InterestException;
import com.springboot.monew.interest.mapper.InterestDtoMapper;
import com.springboot.monew.interest.repository.InterestKeywordRepository;
import com.springboot.monew.interest.repository.InterestRepository;
import com.springboot.monew.interest.repository.KeywordRepository;
import com.springboot.monew.interest.repository.SubscriptionRepository;
import com.springboot.monew.users.entity.User;
import com.springboot.monew.users.exception.UserErrorCode;
import com.springboot.monew.users.exception.UserException;
import com.springboot.monew.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private KeywordRepository keywordRepository;

  @Mock
  private InterestKeywordRepository interestKeywordRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private InterestDtoMapper interestDtoMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("활성 사용자는 관심사 목록을 조회할 수 있다")
  void list_ReturnsInterestPage_WhenUserIsActive() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        "eco",
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    );
    User user = new User("user@example.com", "tester", "password");
    Interest interest = interest(
        UUID.randomUUID(),
        "economy",
        Instant.parse("2026-04-20T00:00:00Z"),
        5
    );
    InterestKeyword interestKeyword = new InterestKeyword(interest,
        keyword(UUID.randomUUID(), "macro"));
    InterestDto interestDto = new InterestDto(interest.getId(), "economy", List.of("macro"), 5,
        true);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findInterests(request)).willReturn(List.of(interest));
    given(interestKeywordRepository.findAllByInterestIdInWithKeyword(List.of(interest.getId())))
        .willReturn(List.of(interestKeyword));
    given(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId,
        List.of(interest.getId())))
        .willReturn(List.of(interest.getId()));
    given(interestDtoMapper.toInterestDto(interest, List.of("macro"), true))
        .willReturn(interestDto);
    given(interestRepository.countInterests("eco")).willReturn(1L);

    // when
    CursorPageResponseInterestDto result = interestService.list(request, userId);

    // then
    assertThat(result.content()).containsExactly(interestDto);
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextAfter()).isNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.totalElements()).isEqualTo(1L);
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("조회 결과가 limit를 초과하면 다음 커서가 포함된다")
  void list_ReturnsCursorPageWithNextCursor_WhenHasNextTrue() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        2
    );
    User user = new User("user@example.com", "tester", "password");
    Interest first = interest(
        UUID.randomUUID(),
        "economy",
        Instant.parse("2026-04-20T00:00:00Z"),
        1
    );
    Interest second = interest(
        UUID.randomUUID(),
        "finance",
        Instant.parse("2026-04-21T00:00:00Z"),
        2
    );
    Interest third = interest(
        UUID.randomUUID(),
        "market",
        Instant.parse("2026-04-22T00:00:00Z"),
        3
    );
    InterestDto firstDto = new InterestDto(first.getId(), "economy", List.of(), 1, false);
    InterestDto secondDto = new InterestDto(second.getId(), "finance", List.of(), 2, false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findInterests(request)).willReturn(List.of(first, second, third));
    given(interestKeywordRepository.findAllByInterestIdInWithKeyword(
        List.of(first.getId(), second.getId())))
        .willReturn(List.of());
    given(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId,
        List.of(first.getId(), second.getId())))
        .willReturn(List.of());
    given(interestDtoMapper.toInterestDto(first, List.of(), false)).willReturn(firstDto);
    given(interestDtoMapper.toInterestDto(second, List.of(), false)).willReturn(secondDto);
    given(interestRepository.countInterests(null)).willReturn(3L);

    // when
    CursorPageResponseInterestDto result = interestService.list(request, userId);

    // then
    assertThat(result.content()).containsExactly(firstDto, secondDto);
    assertThat(result.nextCursor()).isEqualTo("finance");
    assertThat(result.nextAfter()).isEqualTo(second.getCreatedAt());
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3L);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("구독자 수 정렬 조회 결과가 limit를 초과하면 생성 시각을 포함한 다음 커서가 포함된다")
  void list_ReturnsSubscriberCountCursorWithCreatedAt_WhenOrderBySubscriberCountAndHasNextTrue() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        null,
        null,
        2
    );
    User user = new User("user@example.com", "tester", "password");
    Interest first = interest(
        UUID.randomUUID(),
        "popular",
        Instant.parse("2026-04-20T00:00:00Z"),
        5
    );
    Interest second = interest(
        UUID.randomUUID(),
        "middle",
        Instant.parse("2026-04-21T00:00:00Z"),
        3
    );
    Interest third = interest(
        UUID.randomUUID(),
        "low",
        Instant.parse("2026-04-22T00:00:00Z"),
        1
    );
    InterestDto firstDto = new InterestDto(first.getId(), "popular", List.of(), 5, false);
    InterestDto secondDto = new InterestDto(second.getId(), "middle", List.of(), 3, false);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findInterests(request)).willReturn(List.of(first, second, third));
    given(interestKeywordRepository.findAllByInterestIdInWithKeyword(
        List.of(first.getId(), second.getId())))
        .willReturn(List.of());
    given(subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(userId,
        List.of(first.getId(), second.getId())))
        .willReturn(List.of());
    given(interestDtoMapper.toInterestDto(first, List.of(), false)).willReturn(firstDto);
    given(interestDtoMapper.toInterestDto(second, List.of(), false)).willReturn(secondDto);
    given(interestRepository.countInterests(null)).willReturn(3L);

    // when
    CursorPageResponseInterestDto result = interestService.list(request, userId);

    // then
    assertThat(result.content()).containsExactly(firstDto, secondDto);
    assertThat(result.nextCursor()).isEqualTo("3|2026-04-21T00:00:00Z");
    assertThat(result.nextAfter()).isEqualTo(second.getCreatedAt());
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3L);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("조회 결과가 없으면 빈 목록을 반환한다")
  void list_ReturnsEmptyCursorPage_WhenNoInterestsExist() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.subscriberCount,
        InterestDirection.DESC,
        null,
        null,
        10
    );
    User user = new User("user@example.com", "tester", "password");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findInterests(request)).willReturn(List.of());
    given(interestRepository.countInterests(null)).willReturn(0L);

    // when
    CursorPageResponseInterestDto result = interestService.list(request, userId);

    // then
    assertThat(result.content()).isEmpty();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextAfter()).isNull();
    assertThat(result.size()).isZero();
    assertThat(result.totalElements()).isZero();
    assertThat(result.hasNext()).isFalse();

    verify(interestKeywordRepository, never()).findAllByInterestIdInWithKeyword(any());
    verify(subscriptionRepository, never()).findInterestIdsByUserIdAndInterestIdIn(any(), any());
    verify(interestDtoMapper, never()).toInterestDto(any(), any(), anyBoolean());
  }

  @Test
  @DisplayName("존재하지 않는 사용자가 관심사 목록을 조회하면 예외가 발생한다")
  void list_ThrowsException_WhenUserNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    );

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> interestService.list(request, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(interestRepository, never()).findInterests(any());
  }

  @Test
  @DisplayName("삭제된 사용자가 관심사 목록을 조회하면 예외가 발생한다")
  void list_ThrowsException_WhenUserDeleted() {
    // given
    UUID userId = UUID.randomUUID();
    InterestPageRequest request = new InterestPageRequest(
        null,
        InterestOrderBy.name,
        InterestDirection.ASC,
        null,
        null,
        10
    );
    User deletedUser = new User("deleted@example.com", "tester", "password");
    deletedUser.delete();

    given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

    // when
    ThrowingCallable action = () -> interestService.list(request, userId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(UserException.class)
        .satisfies(throwable -> {
          UserException exception = (UserException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("userId", userId));
        });

    verify(interestRepository, never()).findInterests(any());
  }

  @Test
  @DisplayName("관심사를 생성할 수 있다")
  void create_ReturnsInterestDto_WhenValidRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("금융", List.of("증권"));
    Interest savedInterest = interest(
        UUID.randomUUID(),
        "금융",
        Instant.parse("2026-04-20T00:00:00Z"),
        0
    );
    Keyword savedKeyword = keyword(UUID.randomUUID(), "증권");
    InterestDto expected = new InterestDto(savedInterest.getId(), "금융", List.of("증권"), 0, false);

    given(interestRepository.existsByName("금융")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of("경제"));
    given(interestRepository.save(
        argThat(interest -> interest != null && interest.getName().equals("금융"))))
        .willReturn(savedInterest);
    given(keywordRepository.findByName("증권")).willReturn(Optional.of(savedKeyword));
    given(interestDtoMapper.toInterestDto(savedInterest, List.of("증권"), false)).willReturn(
        expected);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(interestKeywordRepository).save(any(InterestKeyword.class));
  }

  @Test
  @DisplayName("기존 키워드를 재사용하여 관심사를 생성할 수 있다")
  void create_ReturnsInterestDto_WhenKeywordAlreadyExists() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("투자", List.of("주식", "채권"));
    Interest savedInterest = interest(
        UUID.randomUUID(),
        "투자",
        Instant.parse("2026-04-20T00:00:00Z"),
        0
    );
    Keyword firstKeyword = keyword(UUID.randomUUID(), "주식");
    Keyword secondKeyword = keyword(UUID.randomUUID(), "채권");
    InterestDto expected = new InterestDto(savedInterest.getId(), "투자", request.keywords(), 0,
        false);

    given(interestRepository.existsByName("투자")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of());
    given(interestRepository.save(any(Interest.class))).willReturn(savedInterest);
    given(keywordRepository.findByName("주식")).willReturn(Optional.of(firstKeyword));
    given(keywordRepository.findByName("채권")).willReturn(Optional.of(secondKeyword));
    given(interestDtoMapper.toInterestDto(savedInterest, request.keywords(), false)).willReturn(
        expected);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(keywordRepository, never()).saveAndFlush(any(Keyword.class));
    verify(interestKeywordRepository, times(2)).save(any(InterestKeyword.class));
  }

  @Test
  @DisplayName("새 키워드를 생성하여 관심사를 생성할 수 있다")
  void create_ReturnsInterestDto_WhenKeywordDoesNotExist() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("부동산", List.of("아파트", "분양"));
    Interest savedInterest = interest(
        UUID.randomUUID(),
        "부동산",
        Instant.parse("2026-04-20T00:00:00Z"),
        0
    );
    Keyword apartment = keyword(UUID.randomUUID(), "아파트");
    Keyword sale = keyword(UUID.randomUUID(), "분양");
    InterestDto expected = new InterestDto(savedInterest.getId(), "부동산", request.keywords(), 0,
        false);

    given(interestRepository.existsByName("부동산")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of("경제"));
    given(interestRepository.save(any(Interest.class))).willReturn(savedInterest);
    given(keywordRepository.findByName("아파트")).willReturn(Optional.empty());
    given(keywordRepository.findByName("분양")).willReturn(Optional.empty());
    given(keywordRepository.saveAndFlush(
        argThat(keyword -> keyword != null && keyword.getName().equals("아파트"))))
        .willReturn(apartment);
    given(keywordRepository.saveAndFlush(
        argThat(keyword -> keyword != null && keyword.getName().equals("분양"))))
        .willReturn(sale);
    given(interestDtoMapper.toInterestDto(savedInterest, request.keywords(), false)).willReturn(
        expected);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(keywordRepository, times(2)).saveAndFlush(any(Keyword.class));
  }

  @Test
  @DisplayName("키워드 저장 중 무결성 예외가 발생해도 기존 키워드를 재조회하여 생성할 수 있다")
  void create_ReturnsInterestDto_WhenKeywordIntegrityViolationOccurs() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("산업", List.of("반도체"));
    Interest savedInterest = interest(
        UUID.randomUUID(),
        "산업",
        Instant.parse("2026-04-20T00:00:00Z"),
        0
    );
    Keyword recoveredKeyword = keyword(UUID.randomUUID(), "반도체");
    InterestDto expected = new InterestDto(savedInterest.getId(), "산업", List.of("반도체"), 0, false);

    given(interestRepository.existsByName("산업")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of());
    given(interestRepository.save(any(Interest.class))).willReturn(savedInterest);
    given(keywordRepository.findByName("반도체")).willReturn(Optional.empty(),
        Optional.of(recoveredKeyword));
    given(keywordRepository.saveAndFlush(any(Keyword.class)))
        .willThrow(new DataIntegrityViolationException("duplicate"));
    given(interestDtoMapper.toInterestDto(savedInterest, List.of("반도체"), false)).willReturn(
        expected);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(keywordRepository).saveAndFlush(any(Keyword.class));
    verify(interestKeywordRepository).save(any(InterestKeyword.class));
  }

  @Test
  @DisplayName("중복된 관심사 이름으로 생성하면 예외가 발생한다")
  void create_ThrowsException_WhenInterestNameDuplicated() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("경제", List.of("증시"));

    given(interestRepository.existsByName("경제")).willReturn(true);

    // when
    ThrowingCallable action = () -> interestService.create(request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(
              InterestErrorCode.INTEREST_NAME_DUPLICATION);
          assertThat(exception.getDetails()).isEqualTo(Map.of("name", "경제"));
        });

    verify(interestRepository, never()).save(any(Interest.class));
  }

  @Test
  @DisplayName("유사한 관심사 이름이 존재하면 예외가 발생한다")
  void create_ThrowsException_WhenSimilarInterestNameExists() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("finance", List.of("stock"));

    given(interestRepository.existsByName("finance")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of("finanse"));

    // when
    ThrowingCallable action = () -> interestService.create(request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(
              InterestErrorCode.INTEREST_NAME_DUPLICATION);
          assertThat(exception.getDetails()).isEqualTo(Map.of("name", "finance"));
        });
  }

  @Test
  @DisplayName("요청 키워드에 중복이 있으면 예외가 발생한다")
  void create_ThrowsException_WhenDuplicateKeywords() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("금융", List.of("주식", "주식"));

    given(interestRepository.existsByName("금융")).willReturn(false);
    given(interestRepository.findAllNames()).willReturn(List.of());

    // when
    ThrowingCallable action = () -> interestService.create(request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.DUPLICATE_KEYWORDS);
          assertThat(exception.getDetails()).isEmpty();
        });
  }

  @Test
  @DisplayName("관심사를 수정할 수 있다")
  void update_ReturnsUpdatedInterestDto_WhenValidRequest() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "투자", Instant.parse("2026-04-20T00:00:00Z"), 0);
    Keyword oldKeyword = keyword(UUID.randomUUID(), "주식");
    Keyword newKeyword = keyword(UUID.randomUUID(), "채권");
    InterestKeyword oldLink = new InterestKeyword(interest, oldKeyword);
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("채권"));
    InterestDto expected = new InterestDto(interestId, "투자", List.of("채권"), 0, false);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(
        List.of(oldLink));
    given(keywordRepository.findByName("채권")).willReturn(Optional.empty());
    given(keywordRepository.saveAndFlush(
        argThat(keyword -> keyword != null && keyword.getName().equals("채권"))))
        .willReturn(newKeyword);
    given(interestDtoMapper.toInterestDto(interest, List.of("채권"), false)).willReturn(expected);

    // when
    InterestDto result = interestService.update(interestId, request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(interestKeywordRepository).deleteAll(List.of(oldLink));
    verify(interestKeywordRepository).save(any(InterestKeyword.class));
  }

  @Test
  @DisplayName("기존 키워드는 유지하고 새 키워드를 추가하여 관심사를 수정할 수 있다")
  void update_ReturnsUpdatedInterestDto_WhenKeepsExistingKeywordsAndAddsNewKeywords() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "금융", Instant.parse("2026-04-20T00:00:00Z"), 0);
    Keyword existingKeyword = keyword(UUID.randomUUID(), "주식");
    Keyword newKeyword = keyword(UUID.randomUUID(), "ETF");
    InterestKeyword existingLink = new InterestKeyword(interest, existingKeyword);
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("주식", "ETF"));
    InterestDto expected = new InterestDto(interestId, "금융", request.keywords(), 0, false);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(
        List.of(existingLink));
    given(keywordRepository.findByName("ETF")).willReturn(Optional.empty());
    given(keywordRepository.saveAndFlush(
        argThat(keyword -> keyword != null && keyword.getName().equals("ETF"))))
        .willReturn(newKeyword);
    given(interestDtoMapper.toInterestDto(interest, request.keywords(), false)).willReturn(
        expected);

    // when
    InterestDto result = interestService.update(interestId, request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(interestKeywordRepository, never()).deleteAll(any());
    verify(interestKeywordRepository).save(any(InterestKeyword.class));
    verify(keywordRepository, never()).deleteOrphanKeywordsByIds(any());
  }

  @Test
  @DisplayName("수정 과정에서 더 이상 사용되지 않는 키워드는 삭제한다")
  void update_DeletesOrphanKeywords_WhenKeywordsAreRemoved() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "산업", Instant.parse("2026-04-20T00:00:00Z"), 0);
    Keyword keepKeyword = keyword(UUID.randomUUID(), "반도체");
    Keyword removeKeyword = keyword(UUID.randomUUID(), "철강");
    InterestKeyword keepLink = new InterestKeyword(interest, keepKeyword);
    InterestKeyword removeLink = new InterestKeyword(interest, removeKeyword);
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("반도체"));
    InterestDto expected = new InterestDto(interestId, "산업", List.of("반도체"), 0, false);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(
        List.of(keepLink, removeLink));
    given(interestDtoMapper.toInterestDto(interest, List.of("반도체"), false)).willReturn(expected);

    // when
    InterestDto result = interestService.update(interestId, request);

    // then
    assertThat(result).isEqualTo(expected);
    verify(interestKeywordRepository).deleteAll(List.of(removeLink));
    verify(keywordRepository).deleteOrphanKeywordsByIds(List.of(removeKeyword.getId()));
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 수정하면 예외가 발생한다")
  void update_ThrowsException_WhenInterestNotFound() {
    // given
    UUID interestId = UUID.randomUUID();
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("채권"));

    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> interestService.update(interestId, request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  @Test
  @DisplayName("수정 요청 키워드에 중복이 있으면 예외가 발생한다")
  void update_ThrowsException_WhenDuplicateKeywords() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "경제", Instant.parse("2026-04-20T00:00:00Z"), 0);
    InterestUpdateRequest request = new InterestUpdateRequest(List.of("증시", "증시"));

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when
    ThrowingCallable action = () -> interestService.update(interestId, request);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.DUPLICATE_KEYWORDS);
          assertThat(exception.getDetails()).isEmpty();
        });

    verify(interestKeywordRepository, never()).findAllByInterestWithKeyword(any());
  }

  @Test
  @DisplayName("관심사를 삭제할 수 있다")
  void delete_DeletesInterest_WhenInterestExists() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "경제", Instant.parse("2026-04-20T00:00:00Z"), 0);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(List.of());

    // when
    interestService.delete(interestId);

    // then
    verify(interestKeywordRepository).deleteAll(List.of());
    verify(interestRepository).delete(interest);
    verify(keywordRepository, never()).deleteOrphanKeywordsByIds(any());
  }

  @Test
  @DisplayName("관심사 삭제 시 연결된 관심사 키워드도 함께 삭제한다")
  void delete_DeletesInterestKeywords_WhenInterestExists() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "부동산", Instant.parse("2026-04-20T00:00:00Z"), 0);
    Keyword keyword = keyword(UUID.randomUUID(), "아파트");
    InterestKeyword link = new InterestKeyword(interest, keyword);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(
        List.of(link));

    // when
    interestService.delete(interestId);

    // then
    verify(interestKeywordRepository).deleteAll(List.of(link));
    verify(interestRepository).delete(interest);
  }

  @Test
  @DisplayName("관심사 삭제 후 더 이상 사용되지 않는 키워드는 삭제한다")
  void delete_DeletesOrphanKeywords_WhenInterestDeleted() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = interest(interestId, "테크", Instant.parse("2026-04-20T00:00:00Z"), 0);
    Keyword firstKeyword = keyword(UUID.randomUUID(), "AI");
    Keyword secondKeyword = keyword(UUID.randomUUID(), "로봇");
    InterestKeyword firstLink = new InterestKeyword(interest, firstKeyword);
    InterestKeyword secondLink = new InterestKeyword(interest, secondKeyword);

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestKeywordRepository.findAllByInterestWithKeyword(interest)).willReturn(
        List.of(firstLink, secondLink));

    // when
    interestService.delete(interestId);

    // then
    verify(keywordRepository).deleteOrphanKeywordsByIds(
        List.of(firstKeyword.getId(), secondKeyword.getId()));
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 삭제하면 예외가 발생한다")
  void delete_ThrowsException_WhenInterestNotFound() {
    // given
    UUID interestId = UUID.randomUUID();

    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when
    ThrowingCallable action = () -> interestService.delete(interestId);

    // then
    assertThatThrownBy(action)
        .isInstanceOf(InterestException.class)
        .satisfies(throwable -> {
          InterestException exception = (InterestException) throwable;
          assertThat(exception.getErrorCode()).isEqualTo(InterestErrorCode.INTEREST_NOT_FOUND);
          assertThat(exception.getDetails()).isEqualTo(Map.of("interestId", interestId));
        });
  }

  private Interest interest(UUID id, String name, Instant createdAt, long subscriberCount) {
    Interest interest = new Interest(name);
    ReflectionTestUtils.setField(interest, "id", id);
    ReflectionTestUtils.setField(interest, "createdAt", createdAt);
    ReflectionTestUtils.setField(interest, "subscriberCount", subscriberCount);
    return interest;
  }

  private Keyword keyword(UUID id, String name) {
    Keyword keyword = new Keyword(name);
    ReflectionTestUtils.setField(keyword, "id", id);
    return keyword;
  }
}
