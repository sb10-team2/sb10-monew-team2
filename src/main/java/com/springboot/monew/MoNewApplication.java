package com.springboot.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//스케줄링 기능을 활성화
//Spring이 내부적으로 스케줄러 인프라를 등록하고 @Scheduled가 붙은 메서드를 찾아서 주기적으로 실행되도록 해준다.
@EnableScheduling
public class MoNewApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoNewApplication.class, args);
  }

}
