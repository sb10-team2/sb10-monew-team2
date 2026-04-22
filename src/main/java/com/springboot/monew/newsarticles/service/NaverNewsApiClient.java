package com.springboot.monew.newsarticles.service;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.NaverNewsResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class NaverNewsApiClient {

//    @Value로 주입받은 필드들은 final이 아니어서 테스트 작성과 의존성 주입이 불편하다.
//    생성자 주입으로 변경하면 테스트시 모킹도 쉽다.
//    @Value("${naver.api.key}")
//    private String clientId;
//
//    @Value("${naver.api.secret}")
//    private String clientSecret;

  private final String clientId;
  private final String clientSecret;
  private final RestClient restClient;

  public NaverNewsApiClient(
      @Value("${naver.api.key}") String clientId,
      @Value("${naver.api.secret}") String clientSecret,
      RestClient restClient) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.restClient = restClient;
  }


  //외부 API 호출의 예외가 배치 전체를 중단시킬 수 있다.
  public List<NaverNewsItem> searchNews(String query) {

    try {
      NaverNewsResponse response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .scheme("https")
              .host("openapi.naver.com")
              .path("/v1/search/news.json")
              .queryParam("query", query)
              .queryParam("display", 10)      //한번에 가져올 개수: 10
              .queryParam("start", 1)         //시작위치
              .queryParam("sort", "date")     //날짜순으로 내림차순 정렬
              .build()
          )
          .header("X-Naver-Client-Id", clientId)
          .header("X-Naver-Client-Secret", clientSecret)
          .retrieve()
          .body(NaverNewsResponse.class);

      return response == null || response.articles() == null ? List.of() : response.articles();

    } catch (RestClientException ex) {
      log.warn("Naver API 호출 실패. query={}", query, ex);
      return List.of();
    }
  }


}
