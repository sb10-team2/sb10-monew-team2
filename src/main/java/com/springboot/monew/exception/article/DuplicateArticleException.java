package com.springboot.monew.exception.article;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class DuplicateArticleException extends ArticleException {
    public DuplicateArticleException(String articleUrl) {
        super(ErrorCode.DUPLICATE_ARTICLE, Map.of("articleUrl", articleUrl));
    }
}
