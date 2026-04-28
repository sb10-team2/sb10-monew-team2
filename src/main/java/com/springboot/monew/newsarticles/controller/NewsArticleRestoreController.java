package com.springboot.monew.newsarticles.controller;

import com.springboot.monew.newsarticles.dto.response.RestoreResultDto;
import com.springboot.monew.newsarticles.s3.NewsArticleRestoreService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

//cloud.aws.s3.enabled: true일때만 S3 Bean 생성 조건 걸어놈. -> /api/articles/restore REST API를 따로 구성
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
@ConditionalOnProperty(name = "cloud.aws.s3.enabled", havingValue = "true")
public class NewsArticleRestoreController {

  private final NewsArticleRestoreService newsArticleRestoreService;

  @GetMapping("/restore")
  public ResponseEntity<List<RestoreResultDto>> restore(@RequestParam(value = "from", required = true) String from,
      @RequestParam(value = "to", required = true) String to){

    LocalDate fromDate;
    LocalDate toDate;
    try {
      fromDate = LocalDate.parse(from.substring(0, 10));
      toDate = LocalDate.parse(to.substring(0, 10));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "날짜 형식이 올바르지 않습니다. (예: 2024-01-15)");
    }

    return ResponseEntity.ok(newsArticleRestoreService.restore(fromDate, toDate));
  }

}
