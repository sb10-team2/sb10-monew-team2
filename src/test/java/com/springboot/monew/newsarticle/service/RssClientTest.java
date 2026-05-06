package com.springboot.monew.newsarticle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.springboot.monew.newsarticle.dto.RssItem;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class RssClientTest {

  private final RssClient rssClient = new RssClient();

  @Test
  @DisplayName("RSS XML 파싱에 성공하면 RssItem 목록을 반환한다")
  void read_ReturnsRssItems_WhenRssXmlIsValid() throws Exception {

    // given
    String url = "https://example.com/rss";

    String rssXml = """
        <rss>
          <channel>
            <item>
              <title>테스트 기사 제목</title>
              <link>https://example.com/news/1</link>
              <description>테스트 기사 요약</description>
              <pubDate>Fri, 17 Apr 2026 11:12:07 +0900</pubDate>
            </item>
          </channel>
        </rss>
        """;

    Document document = Jsoup.parse(rssXml, "", Parser.xmlParser());
    Connection connection = mock(Connection.class);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      // 실제 외부 RSS URL 호출을 막고, Mock Connection을 반환하도록 설정
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);

      // connection.get() 호출 시 미리 만든 XML Document 반환
      when(connection.get()).thenReturn(document);

      // when
      List<RssItem> result = rssClient.read(url);

      // then
      assertThat(result).hasSize(1);

      RssItem item = result.get(0);
      assertThat(item.title()).isEqualTo("테스트 기사 제목");
      assertThat(item.link()).isEqualTo("https://example.com/news/1");
      assertThat(item.description()).isEqualTo("테스트 기사 요약");
      assertThat(item.publishedAt()).isEqualTo(Instant.parse("2026-04-17T02:12:07Z"));
    }
  }

  @Test
  @DisplayName("description 태그가 없으면 빈 문자열로 반환한다")
  void read_ReturnsEmptyDescription_WhenDescriptionMissing() throws Exception {

    // given
    String url = "https://example.com/rss";

    String rssXml = """
        <rss>
          <channel>
            <item>
              <title>제목</title>
              <link>https://example.com/news/1</link>
              <pubDate>Fri, 17 Apr 2026 11:12:07 +0900</pubDate>
            </item>
          </channel>
        </rss>
        """;

    Document document = Jsoup.parse(rssXml, "", Parser.xmlParser());
    Connection connection = mock(Connection.class);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);
      when(connection.get()).thenReturn(document);

      // when
      List<RssItem> result = rssClient.read(url);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).description()).isEqualTo("");
    }
  }

  @Test
  @DisplayName("RSS 연결 또는 파싱 중 예외 발생 시 RuntimeException을 던진다")
  void read_ThrowsRuntimeException_WhenJsoupFails() throws Exception {

    // given
    String url = "https://example.com/rss";

    Connection connection = mock(Connection.class);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {

      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connection);
      when(connection.get()).thenThrow(new IOException("연결 실패"));

      // when & then
      assertThatThrownBy(() -> rssClient.read(url))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("RSS 파싱 실패")
          .hasCauseInstanceOf(IOException.class);
    }
  }


}