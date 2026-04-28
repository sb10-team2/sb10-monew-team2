package com.springboot.monew.newsarticles.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestoreResultDto(
    Instant restoreDate,
    List<UUID> restoredArticleIds,
    Long restoredArticleCount
) {

}
