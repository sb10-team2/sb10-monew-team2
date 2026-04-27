package com.springboot.monew.newsarticles.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.newsarticles.dto.NewsArticleBackupDto;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import com.springboot.monew.newsarticles.service.NewsArticleService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsArticleBackupService {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private final NewsArticleRepository newsArticleRepository;
  private final S3BackupService s3BackupService;
  private final ObjectMapper objectMapper;

  public void backupByPublishedAtDate(LocalDate backupDate) {
    try{
      //한국날짜 기준으로 백업하기 위한 시간 설정
      Instant start = backupDate.atStartOfDay(KOREA_ZONE).toInstant();
      Instant end = backupDate.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant();

      //S3에 뉴스기사 데이터들이 배열형태로 저장
      List<NewsArticleBackupDto> backupDtos = newsArticleRepository.findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan(start, end)
          .stream()
          .map(article -> new NewsArticleBackupDto(
              article.getId(),
              article.getSource(),
              article.getOriginalLink(),
              article.getTitle(),
              article.getPublishedAt(),
              article.getSummary(),
              article.isDeleted(),
              article.getCreatedAt()
          ))
          .toList();

      String json = objectMapper.writeValueAsString(backupDtos);
      String key = "backup/news-articles/%s/news-articles.json".formatted(backupDate);
      s3BackupService.upload(key, json);

    }catch(Exception e){
      throw new RuntimeException("뉴스 기사 백업 실패. date=" + backupDate, e);
    }
  }

}
