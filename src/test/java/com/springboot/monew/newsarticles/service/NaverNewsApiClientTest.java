package com.springboot.monew.newsarticles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.springboot.monew.newsarticles.dto.NaverNewsItem;
import com.springboot.monew.newsarticles.dto.response.NaverNewsResponse;
import com.springboot.monew.newsarticles.exception.ArticleException;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class NaverNewsApiClientTest {

  @Mock
  private RestClient restClient;

  @Mock
  private RestClient.ResponseSpec responseSpec;

  private NaverNewsApiClient naverNewsApiClient;

  private final String clientId = "test-client-id";
  private final String clientSecret = "test-client-secret";

  @BeforeEach
  void setUp() {
    // @Value로 주입되는 값은 테스트에서 직접 생성자에 넣어준다.
    naverNewsApiClient = new NaverNewsApiClient(clientId, clientSecret, restClient);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("네이버 뉴스 검색 - API 응답이 정상일 경우 기사 목록을 반환한다")
  void searchNews_ReturnsArticles_WhenApiResponseIsValid() {

    // given
    // RestClient의 fluent API 체인을 모킹하기 위한 객체들
    RestClient.RequestHeadersUriSpec uriSpec = mockRequestHeadersUriSpec();
    RestClient.RequestHeadersSpec headersSpec = mockRequestHeadersSpec();

    NaverNewsItem item = org.mockito.Mockito.mock(NaverNewsItem.class);
    List<NaverNewsItem> articles = List.of(item);

    NaverNewsResponse response = new NaverNewsResponse(articles);

    // restClient.get() 호출 시 URI 설정 단계 객체를 반환한다.
    given(restClient.get()).willReturn(uriSpec);

    // uri(...) 호출 후 header 설정 단계 객체를 반환한다.
    given(uriSpec.uri(any(Function.class))).willReturn(headersSpec);

    // 첫 번째 header 설정 후 같은 headersSpec을 반환하여 다음 header를 이어서 호출할 수 있게 한다.
    given(headersSpec.header(eq("X-Naver-Client-Id"), eq(clientId)))
        .willReturn(headersSpec);

    // 두 번째 header 설정 후 같은 headersSpec을 반환한다.
    given(headersSpec.header(eq("X-Naver-Client-Secret"), eq(clientSecret)))
        .willReturn(headersSpec);

    // retrieve() 호출 시 응답 처리 객체를 반환한다.
    given(headersSpec.retrieve()).willReturn(responseSpec);

    // body(...) 호출 시 네이버 API 응답 DTO를 반환한다.
    given(responseSpec.body(NaverNewsResponse.class)).willReturn(response);

    // when
    List<NaverNewsItem> result = naverNewsApiClient.searchNews("AI");

    // then
    // API 응답에 들어있던 기사 목록이 그대로 반환되어야 한다.
    assertThat(result).hasSize(1);
    assertThat(result).isEqualTo(articles);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("네이버 뉴스 검색 - API 응답이 null이면 빈 리스트를 반환한다")
  void searchNews_ReturnsEmptyList_WhenResponseIsNull() {

    // given
    RestClient.RequestHeadersUriSpec uriSpec = mockRequestHeadersUriSpec();
    RestClient.RequestHeadersSpec headersSpec = mockRequestHeadersSpec();

    given(restClient.get()).willReturn(uriSpec);
    given(uriSpec.uri(any(Function.class))).willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Id"), eq(clientId)))
        .willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Secret"), eq(clientSecret)))
        .willReturn(headersSpec);
    given(headersSpec.retrieve()).willReturn(responseSpec);

    // 네이버 API 응답 body가 null인 상황
    given(responseSpec.body(NaverNewsResponse.class)).willReturn(null);

    // when
    List<NaverNewsItem> result = naverNewsApiClient.searchNews("AI");

    // then
    // null 응답은 예외가 아니라 빈 리스트로 처리한다.
    assertThat(result).isEmpty();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("네이버 뉴스 검색 - 응답의 articles가 null이면 빈 리스트를 반환한다")
  void searchNews_ReturnsEmptyList_WhenArticlesIsNull() {

    // given
    RestClient.RequestHeadersUriSpec uriSpec = mockRequestHeadersUriSpec();
    RestClient.RequestHeadersSpec headersSpec = mockRequestHeadersSpec();

    NaverNewsResponse response = new NaverNewsResponse(null);

    given(restClient.get()).willReturn(uriSpec);
    given(uriSpec.uri(any(Function.class))).willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Id"), eq(clientId)))
        .willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Secret"), eq(clientSecret)))
        .willReturn(headersSpec);
    given(headersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.body(NaverNewsResponse.class)).willReturn(response);

    // when
    List<NaverNewsItem> result = naverNewsApiClient.searchNews("AI");

    // then
    // articles 필드가 null이어도 빈 리스트로 안전하게 처리한다.
    assertThat(result).isEmpty();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("네이버 뉴스 검색 - RestClientException 발생 시 ArticleException으로 변환한다")
  void searchNews_ThrowsArticleException_WhenRestClientExceptionOccurs() {

    // given
    RestClient.RequestHeadersUriSpec uriSpec = mockRequestHeadersUriSpec();
    RestClient.RequestHeadersSpec headersSpec = mockRequestHeadersSpec();

    given(restClient.get()).willReturn(uriSpec);
    given(uriSpec.uri(any(Function.class))).willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Id"), eq(clientId)))
        .willReturn(headersSpec);
    given(headersSpec.header(eq("X-Naver-Client-Secret"), eq(clientSecret)))
        .willReturn(headersSpec);

    // 외부 API 호출 중 RestClientException이 발생하는 상황
    given(headersSpec.retrieve())
        .willThrow(new RestClientException("Naver API request failed"));

    // when & then
    // 외부 API 예외는 ArticleException으로 변환되어야 한다.
    assertThatThrownBy(() -> naverNewsApiClient.searchNews("AI"))
        .isInstanceOf(ArticleException.class);
  }

  @SuppressWarnings("rawtypes")
  private RestClient.RequestHeadersUriSpec mockRequestHeadersUriSpec() {
    return org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
  }

  @SuppressWarnings("rawtypes")
  private RestClient.RequestHeadersSpec mockRequestHeadersSpec() {
    return org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
  }
}