package com.springboot.monew.newsarticles.dto;

import com.springboot.monew.newsarticles.dto.response.CollectedArticle;
import java.util.Set;
import java.util.UUID;

public record CollectedArticleWithInterest(
    CollectedArticle article,
    Set<UUID> interestIds
) {

}
