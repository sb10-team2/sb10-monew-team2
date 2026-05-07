package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "generator")
public record GeneratorProperties(
    int article,
    int interest,
    int user,
    int keyword,
    int interestPerUser,
    int notificationPerUser,
    int commentLikePerUser,
    int commentPerUser,
    int dbBatchSize,
    int articlePerUser,
    int interestPerArticle,
    int keywordPerInterest) {

}
