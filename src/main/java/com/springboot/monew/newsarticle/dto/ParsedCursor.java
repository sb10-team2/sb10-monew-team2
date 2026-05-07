package com.springboot.monew.newsarticle.dto;

import java.time.Instant;

//QueryDSL에서 파싱용 cursor
public record ParsedCursor(
    String value,
    Instant after
) {

}
