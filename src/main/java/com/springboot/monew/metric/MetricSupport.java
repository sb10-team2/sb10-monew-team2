package com.springboot.monew.metric;

import java.time.Duration;
import java.util.Locale;

// 메트릭 기록 전 공통 보정 로직 모음
public final class MetricSupport {

  // null 또는 음수 Duration을 0초 Duration으로 보정
  public static Duration safeDuration(Duration duration) {
    if (duration == null || duration.isNegative()) {
      return Duration.ZERO;
    }
    return duration;
  }

  // Counter와 Gauge에 기록할 음수 값을 0으로 보정
  public static long nonNegative(long amount) {
    return Math.max(0L, amount);
  }

  // 비어 있는 문자열 태그 값을 unknown으로 보정
  public static String tagValue(String value) {
    if (value == null || value.isBlank()) {
      return MonewMetricTags.UNKNOWN;
    }
    return value;
  }

  // enum 태그 값을 Prometheus 표기용 소문자 문자열로 변환
  public static String enumTag(Enum<?> value) {
    if (value == null) {
      return MonewMetricTags.UNKNOWN;
    }
    return value.name().toLowerCase(Locale.ROOT);
  }

  private MetricSupport() {
  }
}
