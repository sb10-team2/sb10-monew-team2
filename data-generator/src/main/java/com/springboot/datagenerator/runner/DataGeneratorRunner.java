package com.springboot.datagenerator.runner;

import com.springboot.datagenerator.orchestrator.GeneratorOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataGeneratorRunner implements ApplicationRunner {

  private final GeneratorOrchestrator generatorOrchestrator;
  private final ApplicationContext context;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    System.out.println("data generator start");
    generatorOrchestrator.run();
    System.out.println("finished");

    int exitCode = SpringApplication.exit(context, () -> 0);
    System.exit(exitCode);
  }
}
