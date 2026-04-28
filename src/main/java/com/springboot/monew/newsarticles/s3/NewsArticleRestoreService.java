package com.springboot.monew.newsarticles.s3;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.monew.newsarticles.dto.NewsArticleBackupDto;
import com.springboot.monew.newsarticles.dto.response.RestoreResultDto;
import com.springboot.monew.newsarticles.entity.NewsArticle;
import com.springboot.monew.newsarticles.repository.NewsArticleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleRestoreService {

  private final S3BackupService s3BackupService;
  private final NewsArticleRepository newsArticleRepository;
  private final ObjectMapper objectMapper;

  //기간(from ~ to) 동안의 뉴스 기사 복구
  public List<RestoreResultDto> restore(LocalDate from, LocalDate to) {

    if (from == null || to == null) {
      throw new IllegalArgumentException("from/to는 null일 수 없습니다.");
    }
    if (from.isAfter(to)){
      throw new IllegalArgumentException("from은 to보다 이후일 수 없습니다.");
    }
    log.debug("[뉴스기사 복구 시작] from={}, to={}", from, to);
    List<RestoreResultDto> results = new ArrayList<>();

    //from: 2026-04-25
    //to: 2026-04-27
    //2026-04-25: true
    //2026-04-26: true
    //2026-04-27: false
    for(LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      RestoreResultDto result = restoreOneDate(date);
      results.add(result);
      log.debug("  date={}, restoredCount={}, restoredIds={}",
          date,
          result.restoredArticleIds().size(),
          result.restoredArticleIds()
      );
    }

    return results;
  }

  //특정 날짜 복구
  private RestoreResultDto restoreOneDate(LocalDate date) {
    log.debug("date형태={}", date);
    try{
      String key = "backup/news-articles/%s/news-articles.json".formatted(date);
      log.debug("key형태={}", key);

      //1. 백업 파일 존재 확인
      if(!s3BackupService.exists(key)) {
        return new RestoreResultDto(Instant.now(), List.of(), 0L);
      }

      //2. JSON 다운로드
      String json = s3BackupService.downloadJson(key);

      //3. JSON -> DTO 리스트 변환
      //readValue(데이터, 타입정보)
      //List 안에 NewsArticleBackupDto가 들어있다
      List<NewsArticleBackupDto> backupDtos = objectMapper.readValue(json, new TypeReference<List<NewsArticleBackupDto>>() {});

      if (backupDtos.isEmpty()) {
        return new RestoreResultDto(Instant.now(), List.of(), 0L);
      }

      //4. originalLink 목록 추출
      List<String> links = backupDtos.stream()
          .map(NewsArticleBackupDto::originalLink)
          .toList();

      //5. DB에 존재하는 링크 조회
      Set<String> existingLinks = newsArticleRepository.findAllByOriginalLinkIn(links).stream()
          .map(NewsArticle::getOriginalLink)
          .collect(Collectors.toSet());

      //6. 유실된 데이터만 필터링
      List<NewsArticle> lostArticles = backupDtos.stream()
          .filter(dto -> Boolean.TRUE.equals(dto.isDeleted()))
          .filter(dto -> !existingLinks.contains(dto.originalLink()))
          .map(dto -> new NewsArticle(
              dto.source(),
              dto.originalLink(),
              dto.title(),
              dto.publishedAt(),
              dto.summary()
          ))
          .toList();

      log.debug("유실된 뉴스기사 데이터={}", lostArticles);

      //7. 저장
      List<NewsArticle> saved = newsArticleRepository.saveAll(lostArticles);

      //8. 결과생성
      List<UUID> retoredIds = saved.stream()
          .map(NewsArticle::getId)
          .toList();

      return new RestoreResultDto(
          Instant.now(),
          retoredIds,
          (long) retoredIds.size()
      );

    }catch(Exception e) {
      throw new RuntimeException("뉴스기사 복구 실패. date= " + date, e);
    }
  }
}
