package com.springboot.monew.newsarticles.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.newsarticles.metric.result.NewsBackupFileResult;
import com.springboot.monew.newsarticles.dto.NewsArticleBackupDto;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleBackupService {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private final NewsArticleRepository newsArticleRepository;
  private final S3BackupService s3BackupService;
  private final ObjectMapper objectMapper;

  public NewsBackupFileResult backupByPublishedAtDate(LocalDate backupDate) {
    try {
      //한국날짜 기준으로 백업하기 위한 시간 설정
      Instant start = backupDate.atStartOfDay(KOREA_ZONE).toInstant();
      Instant end = backupDate.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant();

      //S3에 뉴스기사 데이터들이 배열형태로 저장
      List<NewsArticleBackupDto> backupDtos = newsArticleRepository.findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan(
              start, end)
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
      return NewsBackupFileResult.uploaded(backupDtos.size(),
          json.getBytes(StandardCharsets.UTF_8).length);

    } catch (Exception e) {
      throw new RuntimeException("뉴스 기사 백업 실패. date=" + backupDate, e);
    }
  }

  public NewsBackupFileResult backupIfMissing(LocalDate backupDate) {
    String key = "backup/news-articles/%s/news-articles.json".formatted(backupDate);

    if (s3BackupService.exists(key)) {
      log.info("이미 백업 파일이 존재합니다. backupDate={}, key={}", backupDate, key);
      return NewsBackupFileResult.skippedByExistingFile();
    }
    return backupByPublishedAtDate(backupDate);
  }

}
