package com.springboot.monew.exception.article;

import com.springboot.monew.exception.ErrorCode;
import com.springboot.monew.exception.MonewException;

import java.util.Map;

public abstract class ArticleException extends MonewException {
    public ArticleException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
