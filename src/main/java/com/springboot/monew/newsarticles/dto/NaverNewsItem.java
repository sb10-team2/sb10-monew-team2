package com.springboot.monew.newsarticles.dto;

public record NaverNewsItem(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {

}
