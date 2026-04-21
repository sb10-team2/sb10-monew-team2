package com.springboot.monew.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
public final class NotificationFindRequest {

  private final UUID cursor;
  private final Instant after;
  @Min(1)
  @NotNull
  private final int limit;
  @NotNull
  private final UUID userId;

  public NotificationFindRequest(
      @JsonProperty("cursor") UUID cursor,
      @JsonProperty("after") Instant after,
      @JsonProperty("limit") int limit,
      @JsonProperty("Monew-Request-User-ID") UUID userId) {
    this.cursor = cursor;
    this.after = after;
    this.limit = limit;
    this.userId = userId;
  }
}
