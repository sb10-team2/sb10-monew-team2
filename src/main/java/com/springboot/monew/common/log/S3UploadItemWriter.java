package com.springboot.monew.common.log;

import java.io.File;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

// Todo: 아직 미구현 , S3 upload writer
public class S3UploadItemWriter implements ItemWriter<File> {

  // private final S3Client s3Client;

  @Override
  public void write(Chunk<? extends File> gzipFiles) throws Exception {

  }

  private String buildS3Key(String fileName) {
    return null;
  }
}
