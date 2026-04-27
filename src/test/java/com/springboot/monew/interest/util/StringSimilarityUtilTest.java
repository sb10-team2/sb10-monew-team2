package com.springboot.monew.interest.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StringSimilarityUtilTest {

  @Test
  @DisplayName("null이 포함되면 유사도는 0.0이다")
  void similarity_ReturnsZero_WhenEitherValueIsNull() {
    // given
    String value = "finance";

    // when
    double leftNullResult = StringSimilarityUtil.similarity(null, value);
    double rightNullResult = StringSimilarityUtil.similarity(value, null);

    // then
    assertThat(leftNullResult).isZero();
    assertThat(rightNullResult).isZero();
  }

  @Test
  @DisplayName("두 문자열이 모두 비어 있으면 유사도는 1.0이다")
  void similarity_ReturnsOne_WhenBothValuesAreEmpty() {
    // given
    String first = "";
    String second = "";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isEqualTo(1.0);
  }

  @Test
  @DisplayName("영어 문자열이 완전히 같으면 유사도는 1.0이다")
  void similarity_ReturnsOne_WhenEnglishWordsAreIdentical() {
    // given
    String first = "finance";
    String second = "finance";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isEqualTo(1.0);
  }

  @Test
  @DisplayName("한글 문자열이 완전히 같으면 유사도는 1.0이다")
  void similarity_ReturnsOne_WhenKoreanWordsAreIdentical() {
    // given
    String first = "경제뉴스";
    String second = "경제뉴스";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isEqualTo(1.0);
  }

  @Test
  @DisplayName("영어 문자열의 한 글자만 다르면 기대한 비율로 계산된다")
  void similarity_ReturnsExpectedRatio_WhenEnglishWordsDifferByOneCharacter() {
    // given
    String first = "finance";
    String second = "finanse";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isEqualTo(6.0 / 7.0);
  }

  @Test
  @DisplayName("한글 문자열의 한 글자만 다르면 기대한 비율로 계산된다")
  void similarity_ReturnsExpectedRatio_WhenKoreanWordsDifferByOneCharacter() {
    // given
    String first = "경제뉴스";
    String second = "경제뉴수";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isEqualTo(3.0 / 4.0);
  }

  @Test
  @DisplayName("영어 문자열이 많이 다르면 유사도는 낮다")
  void similarity_ReturnsLowValue_WhenEnglishWordsAreDifferent() {
    // given
    String first = "finance";
    String second = "sports";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isLessThan(0.5);
  }

  @Test
  @DisplayName("한글 문자열이 많이 다르면 유사도는 낮다")
  void similarity_ReturnsLowValue_WhenKoreanWordsAreDifferent() {
    // given
    String first = "경제";
    String second = "스포츠";

    // when
    double result = StringSimilarityUtil.similarity(first, second);

    // then
    assertThat(result).isLessThan(0.5);
  }

  @Test
  @DisplayName("영어 문자열이 기준치 이상이면 유사하다고 판단한다")
  void isSimilarEnough_ReturnsTrue_WhenEnglishSimilarityIsAboveThreshold() {
    // given
    String first = "finance";
    String second = "finanse";
    double threshold = 0.8;

    // when
    boolean result = StringSimilarityUtil.isSimilarEnough(first, second, threshold);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("한글 문자열이 기준치 이상이면 유사하다고 판단한다")
  void isSimilarEnough_ReturnsTrue_WhenKoreanSimilarityIsAboveThreshold() {
    // given
    String first = "경제뉴스";
    String second = "경제뉴수";
    double threshold = 0.7;

    // when
    boolean result = StringSimilarityUtil.isSimilarEnough(first, second, threshold);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("영어 문자열이 기준치 미만이면 유사하지 않다고 판단한다")
  void isSimilarEnough_ReturnsFalse_WhenEnglishSimilarityIsBelowThreshold() {
    // given
    String first = "finance";
    String second = "sports";
    double threshold = 0.8;

    // when
    boolean result = StringSimilarityUtil.isSimilarEnough(first, second, threshold);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("한글 문자열이 기준치 미만이면 유사하지 않다고 판단한다")
  void isSimilarEnough_ReturnsFalse_WhenKoreanSimilarityIsBelowThreshold() {
    // given
    String first = "경제";
    String second = "스포츠";
    double threshold = 0.8;

    // when
    boolean result = StringSimilarityUtil.isSimilarEnough(first, second, threshold);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("영문과 한글이 섞여 있어도 같은 규칙으로 계산한다")
  void similarity_ReturnsSimilarEnoughResult_WhenLanguagesAreMixed() {
    // given
    String first = "finance금융";
    String second = "finanse금융";
    double threshold = 0.8;

    // when
    double similarity = StringSimilarityUtil.similarity(first, second);
    boolean similarEnough = StringSimilarityUtil.isSimilarEnough(first, second, threshold);

    // then
    assertThat(similarity).isGreaterThanOrEqualTo(threshold);
    assertThat(similarEnough).isTrue();
  }
}
