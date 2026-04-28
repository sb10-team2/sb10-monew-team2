package com.springboot.monew.common.log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

public class LogFileItemReader implements ItemReader<File> {

  private final String targetDate;
  private final List<File> logFiles = new ArrayList<>();
  private int index = 0;

  public LogFileItemReader(String targetDate) {
    this.targetDate = targetDate;
  }

  @BeforeStep
  public void beforeStep() {
    String logDir = ".logs/";

    // 어제 날짜 로그 파일만 수집
    File[] files = new File(logDir).listFiles(
        f -> f.getName().contains(targetDate) && f.getName().endsWith(".log")
    );

    if (files != null) {
      logFiles.addAll(Arrays.asList(files));
    }
  }

  @Override
  public File read(){
    if(index < logFiles.size()){
      return logFiles.get(index ++);
    }
    return null;
  }
}
