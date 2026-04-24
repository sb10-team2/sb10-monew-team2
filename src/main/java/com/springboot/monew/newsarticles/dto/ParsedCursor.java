package com.springboot.monew.newsarticles.dto;

import java.time.Instant;

//QueryDSL에서 파싱용 cursor
public record ParsedCursor(
    String value,
    Instant after
) {

}
