package com.springboot.monew.newsarticle.dto;

import java.time.Instant;

public record RssItem(
    String title,
    String link,
    String description,
    Instant publishedAt
) {

}
