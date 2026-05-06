package com.springboot.monew.newsarticle.dto;

public record NaverNewsItem(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {

}
