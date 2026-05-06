package com.springboot.monew.newsarticle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.springboot.monew.newsarticle.dto.NaverNewsItem;
import java.util.List;

//Naver API로 수집한 뉴스기사 data ResponseDto
public record NaverNewsResponse(
    @JsonProperty("items")
    List<NaverNewsItem> articles
) {

}
