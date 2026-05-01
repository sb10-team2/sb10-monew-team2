package com.springboot.datagenerator.runner;

import com.springboot.datagenerator.orchestrator.GeneratorOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataGeneratorRunner implements ApplicationRunner {
  private final GeneratorOrchestrator generatorOrchestrator;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    generatorOrchestrator.run();
  }
}
