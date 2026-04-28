package com.springboot.monew.testdata;

import com.springboot.monew.testdata.entity.UserGenerator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 부하 테스트를 위한 데이터 생성 클래스
 *
 */
@Disabled
@Import({TestDataConfig.class})
@ActiveProfiles("test-data")
@SpringBootTest
public class LoadTestDataRunner {

  @Autowired
  private UserGenerator userGenerator;

  @Test
  void run() {
    userGenerator.run(5000);
  }
}
