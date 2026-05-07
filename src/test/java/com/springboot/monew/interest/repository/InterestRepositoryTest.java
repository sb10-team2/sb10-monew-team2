package com.springboot.monew.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.monew.common.repository.BaseRepositoryTest;
import com.springboot.monew.interest.entity.Interest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class InterestRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Test
  @DisplayName("관심사의 구독자 수를 증가시킬 수 있다")
  void incrementSubscriberCount_IncreasesSubscriberCount_WhenInterestExists() {
    // given
    Interest interest = saveInterest("increase");
    flushAndClear();

    // when
    int updated = interestRepository.incrementSubscriberCount(interest.getId());
    flushAndClear();

    // then
    assertThat(updated).isEqualTo(1);
    assertThat(interestRepository.findSubscriberCountById(interest.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("관심사의 구독자 수를 감소시킬 수 있다")
  void decrementSubscriberCount_DecreasesSubscriberCount_WhenSubscriberCountIsPositive() {
    // given
    Interest interest = saveInterest("decrease");
    setSubscriberCount(interest, 2);
    flushAndClear();

    // when
    int updated = interestRepository.decrementSubscriberCount(interest.getId());
    flushAndClear();

    // then
    assertThat(updated).isEqualTo(1);
    assertThat(interestRepository.findSubscriberCountById(interest.getId())).isEqualTo(1L);
  }

  @Test
  @DisplayName("관심사의 구독자 수는 0 미만으로 감소하지 않는다")
  void decrementSubscriberCount_KeepsZero_WhenSubscriberCountIsZero() {
    // given
    Interest interest = saveInterest("zero");
    flushAndClear();

    // when
    int updated = interestRepository.decrementSubscriberCount(interest.getId());
    flushAndClear();

    // then
    assertThat(updated).isEqualTo(1);
    assertThat(interestRepository.findSubscriberCountById(interest.getId())).isZero();
  }

  @Test
  @DisplayName("관심사의 구독자 수를 조회할 수 있다")
  void findSubscriberCountById_ReturnsSubscriberCount_WhenInterestExists() {
    // given
    Interest interest = saveInterest("subscriber-count");
    setSubscriberCount(interest, 7);
    flushAndClear();

    // when
    Long result = interestRepository.findSubscriberCountById(interest.getId());

    // then
    assertThat(result).isEqualTo(7L);
  }

  private Interest saveInterest(String name) {
    Interest interest = new Interest(name);
    em.persist(interest);
    em.flush();
    return interest;
  }

  private void setSubscriberCount(Interest interest, long subscriberCount) {
    ReflectionTestUtils.setField(interest, "subscriberCount", subscriberCount);
    em.flush();
  }
}
