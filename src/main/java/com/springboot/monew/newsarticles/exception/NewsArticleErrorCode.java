package com.springboot.monew.newsarticles.exception;

import com.springboot.monew.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NewsArticleErrorCode implements ErrorCode {
  NEWS_ARTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NA01", "뉴스기사를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public String getCode() {
    return code;
  }
}
