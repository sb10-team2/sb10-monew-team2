package com.springboot.monew.newsarticles.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import java.util.List;

//Naver API로 수집한 뉴스기사 data ResponseDto
public record NaverNewsResponse(
    @JsonProperty("items")
    List<NaverNewsItem> articles
) {

}
