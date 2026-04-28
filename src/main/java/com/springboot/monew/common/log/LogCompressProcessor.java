package com.springboot.monew.common.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.springframework.batch.item.ItemProcessor;

// 로그 파일 gzip으로 압축
public class LogCompressProcessor implements ItemProcessor<File, File> {

  @Override
  public File process(File logFile) {
    // File 전처리 (gzip으로 압축, 업로드 비용 절감 + S3 저장 비용 절감)
    File gzipFile = new File(logFile.getPath() + ".gz");

    try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipFile));
        FileInputStream fis = new FileInputStream(logFile)) {
      fis.transferTo(gos);
    } catch (IOException e) {
      // 실패 시 불완전한 gz 파일 삭제
      if (gzipFile.exists()) {
        gzipFile.delete();
      }
      throw new RuntimeException("로그 파일 압축 실패: " + logFile.getName(), e);
    }

    return gzipFile;
  }
}
