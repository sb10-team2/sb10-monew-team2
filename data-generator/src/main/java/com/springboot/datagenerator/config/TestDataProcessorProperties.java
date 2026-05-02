package com.springboot.datagenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test-data-processor")
public record TestDataProcessorProperties(int batchSize, String commonUserId) {

}
