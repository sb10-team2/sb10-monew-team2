package com.springboot.monew.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeConverter {

  public static LocalDateTime toDatetime(Instant time) {
    return LocalDateTime.ofInstant(time, ZoneId.systemDefault());
  }
}
