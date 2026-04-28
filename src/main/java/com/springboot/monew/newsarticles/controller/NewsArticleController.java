package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.newsarticles.dto.request.NewsArticlePageRequest;
import com.springboot.monew.newsarticles.dto.response.CursorPageResponseNewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleDto;
import com.springboot.monew.newsarticles.dto.response.NewsArticleViewDto;
import com.springboot.monew.newsarticles.dto.response.RestoreResultDto;
import com.springboot.monew.newsarticles.enums.ArticleSource;
import com.springboot.monew.newsarticles.service.NewsArticleCollectService;
import com.springboot.monew.newsarticles.s3.NewsArticleRestoreService;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "뉴스 관리", description = "뉴스 기사 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class NewsArticleController implements NewsArticleApiDocs {

  private final NewsArticleCollectService newsArticleCollectService;
  private final NewsArticleService newsArticleService;
  private final NewsArticleRestoreService  newsArticleRestoreService;

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

  //뉴스기사 목록 조회
  @GetMapping
  public CursorPageResponseNewsArticleDto list(@Valid NewsArticlePageRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID userId){
    return newsArticleService.list(request, userId);

  }

  //뉴스기사 단건 조회
  @GetMapping("/{articleId}")
  public ResponseEntity<NewsArticleDto> find(@PathVariable("articleId") UUID articleId,
                                                 @RequestHeader("Monew-Request-User-ID") UUID userId){

    NewsArticleDto newsArticleDto = newsArticleService.findById(articleId, userId);
    return ResponseEntity.ok(newsArticleDto);

  }

  //출처 목록 조회
  @GetMapping("/sources")
  public ResponseEntity<List<ArticleSource>> findSource(){
    return ResponseEntity.ok(newsArticleService.findAllSources());
  }

  @GetMapping("/restore")
  public ResponseEntity<List<RestoreResultDto>> restore(@RequestParam(value = "from", required = true) String from,
                                                        @RequestParam(value = "to", required = true) String to){

    LocalDate fromDate = LocalDate.parse(from.substring(0, 10));
    LocalDate toDate = LocalDate.parse(to.substring(0, 10));
    List<RestoreResultDto> result = newsArticleRestoreService.restore(fromDate, toDate);

    return ResponseEntity.ok(result);
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
