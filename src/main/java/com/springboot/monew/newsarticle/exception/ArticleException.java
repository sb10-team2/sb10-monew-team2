package com.springboot.monew.newsarticle.exception;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;
import java.util.Map;

public class ArticleException extends MonewException {

  public ArticleException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
