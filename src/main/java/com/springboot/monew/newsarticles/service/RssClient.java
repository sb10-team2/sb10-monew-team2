package com.springboot.monew.newsarticles.service;

import com.springboot.monew.newsarticles.dto.RssItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RssClient {

    public List<RssItem> read(String url) {
        try {
            // XML 전체를 Document 객체로 가져온다.
            Document doc = Jsoup.connect(url).get();

            List<RssItem> items = new ArrayList<>();

            //XML에서 item 태그를 하나씩 가져와서 for문 돌면서 title,link,description등으로 파싱.
            for (Element item : doc.select("item")) {

                String title = item.selectFirst("title").text();
                String link = item.selectFirst("link").text();
                String description = item.selectFirst("description").text();
                String pubDate = item.selectFirst("pubDate").text();

                //RssItem형으로 변환해서 반환
                items.add(new RssItem(
                        title,
                        link,
                        description,
                        parseDate(pubDate)
                ));
            }

            return items;

        } catch (Exception e) {
            throw new RuntimeException("RSS 파싱 실패", e);
        }
    }

    //Fri, 17 Apr 2026 11:12:07 +0900 이런 날짜를
    //2026-04-17T10:30:00+09:00 이런형식으로 변환해준다.
    private Instant parseDate(String pubDate) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

        return ZonedDateTime.parse(pubDate, formatter).toInstant();
    }
}
