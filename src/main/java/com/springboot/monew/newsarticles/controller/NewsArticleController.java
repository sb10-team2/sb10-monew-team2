package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class NewsArticleController {

  private final NewsArticleCollectService newsArticleCollectService;
  private final NewsArticleService newsArticleService;

  @PostMapping
  public ResponseEntity<String> collectNews() {
    newsArticleCollectService.collectAll();
    return ResponseEntity.ok("뉴스 수집 완료");
  }

  //기사 뷰 등록
  @PostMapping("/{articleId}/article-views")
  public ResponseEntity<NewsArticleViewDto> createView(@PathVariable("articleId") UUID articleId,
                                                       @RequestHeader("Monew-Request-User-ID") UUID userId) {

    NewsArticleViewDto newsArticleViewDto = newsArticleService.createView(articleId, userId);

    //헤더: 생성된 리소스의 URL
    return ResponseEntity.created(URI.create("/api/articles/" + articleId + "/article-views/" + newsArticleViewDto.id())).body(newsArticleViewDto);

  }

  //뉴스기사 논리삭제
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> softDelete(@PathVariable UUID articleId) {

    newsArticleService.softDelete(articleId);

    return ResponseEntity.noContent().build();
  }

  //뉴스기사 물리삭제
  @DeleteMapping("/{articleId}/hard")
  public ResponseEntity<Void> hardDelete(@PathVariable("articleId") UUID articleId) {

    newsArticleService.hardDelete(articleId);

    //삭제 성공시 204
    return ResponseEntity.noContent().build();

  }
}
