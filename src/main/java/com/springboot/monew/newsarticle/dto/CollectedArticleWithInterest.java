package com.springboot.monew.newsarticle.dto;

import com.springboot.monew.newsarticle.dto.response.CollectedArticle;
import java.util.Set;
import java.util.UUID;

public record CollectedArticleWithInterest(
    CollectedArticle article,
    Set<UUID> interestIds
) {

}
