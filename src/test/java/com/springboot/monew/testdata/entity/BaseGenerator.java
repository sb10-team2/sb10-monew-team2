package com.springboot.monew.testdata.entity;

import com.springboot.monew.common.entity.BaseEntity;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.datafaker.Faker;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseGenerator<T extends BaseEntity> {

  @Setter
  protected int batchSize = 1000;

  protected static final ThreadLocal<Faker> faker = ThreadLocal.withInitial(Faker::new);
  protected final JdbcTemplate template;
  protected final Executor executor;

  protected void executeBatch(List<T> entities) {
    template.batchUpdate(sql(), entities, batchSize, this::setValues);
  }

  protected List<UUID> generate(int size, Function<Integer, List<T>> generator) {
    List<CompletableFuture<List<UUID>>> futures = new ArrayList<>();

    int numTasks = (int) Math.ceil((double) size / batchSize);
    for (int i = 0; i < numTasks; i++) {
      int chunkSize = Math.min(batchSize, size - (i * batchSize));
      futures.add(CompletableFuture.supplyAsync(() -> doAsync(chunkSize, generator), executor));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().flatMap(f -> f.join().stream()).toList();
  }

  protected abstract void setValues(PreparedStatement ps, T entity) throws SQLException;

  protected abstract String sql();

  protected Instant timestamp() {
    return Instant.now().minus(14, ChronoUnit.DAYS);
  }

  private List<UUID> doAsync(int chunkSize, Function<Integer, List<T>> generator) {
    List<T> chunk = generator.apply(chunkSize);
    executeBatch(chunk);
    return chunk.stream().map(BaseEntity::getId).toList();
  }
}
