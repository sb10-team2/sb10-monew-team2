package com.springboot.monew.testdata.entity;

import com.springboot.monew.common.entity.BaseEntity;
import com.springboot.monew.interest.entity.Keyword;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.datafaker.Faker;
import org.instancio.generator.specs.InstantGeneratorSpec;
import org.instancio.generators.Generators;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public abstract class BaseGenerator<T extends BaseEntity> {

  private static final int DB_BATCH_SIZE = 1000;

  protected static final ThreadLocal<Faker> faker = ThreadLocal.withInitial(Faker::new);
  protected final JdbcTemplate template;
  protected final Executor executor;
  protected final Instant now = Instant.now();
  protected final Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
  protected final Instant twoWeeksAgo = weekAgo.minus(7, ChronoUnit.DAYS);

  protected void executeBatch(List<T> entities) {
    template.batchUpdate(sql(), entities, DB_BATCH_SIZE, this::setValues);
  }

  protected List<T> generate(int size, Function<Integer, List<T>> generator) {
    List<CompletableFuture<List<T>>> futures = new ArrayList<>();

    int numTasks = (int) Math.ceil((double) size / batchSize());
    for (int i = 0; i < numTasks; i++) {
      int chunkSize = Math.min(batchSize(), size - (i * batchSize()));
      futures.add(CompletableFuture.supplyAsync(() -> doAsync(chunkSize, generator), executor));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().flatMap(f -> f.join().stream()).toList();
  }

  protected InstantGeneratorSpec betweenNowAndTwoWeeksAgo(Generators gen) {
    return gen.temporal().instant().range(twoWeeksAgo, now);
  }

  protected InstantGeneratorSpec betweenWeekAndTwoWeeksAgo(Generators gen) {
    return gen.temporal().instant().range(twoWeeksAgo, weekAgo);
  }

  protected InstantGeneratorSpec betweenNowAndWeekAgo(Generators gen) {
    return gen.temporal().instant().range(weekAgo, now);
  }

  protected Set<Integer> uniqueRandomNumbers(List<Keyword> keywords, int size) {
    Set<Integer> uniqueIndices = new HashSet<>();
    while (uniqueIndices.size() < size) {
      uniqueIndices.add(ThreadLocalRandom.current().nextInt(keywords.size()));
    }
    return uniqueIndices;
  }

  protected abstract void setValues(PreparedStatement ps, T entity) throws SQLException;

  protected abstract String sql();

  protected int batchSize() {
    return DB_BATCH_SIZE;
  }

  protected String insertSql(String table, String... fields) {
    String columns = String.join(",", fields);
    String values = String.join(",", Stream.generate(() -> "?").limit(fields.length).toList());
    return "insert into %s (%s) values (%s)".formatted(table, columns, values);
  }

  private List<T> doAsync(int chunkSize, Function<Integer, List<T>> generator) {
    List<T> chunk = generator.apply(chunkSize);
    executeBatch(chunk);
    return chunk;
  }
}
