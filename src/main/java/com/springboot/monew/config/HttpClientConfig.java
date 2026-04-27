package com.springboot.monew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

//설정 클래스
//스프링이 실행될때 가장 먼저 읽어서 앱에 필요한 도구(Bean)을 여기 만들테니 기억해달라고 스프링에게 알려줌
@Configuration
public class HttpClientConfig {

  //공용도구로 등록
  //Spring은 RestClient라는 도구를 써서 네이버 서버에 접속해서 뉴스 데이터(JSON)를 받아온다.
  //new RestClient처럼 한번에 만드는게 아니라.
  //Builder를 사용해서 조립단계를 거쳐서 객체를 만든다.
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
  }
}
