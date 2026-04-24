/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Application entrypoint. */
@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {
  private final HenkiloRepository henkiloRepository;
  private final OnrUpdateService onrService;
  private final BatchTaskProperties taskProperties;

  static void main(String[] args) {
    new SpringApplication(Application.class).run(args);
  }

  Application(
      HenkiloRepository henkiloRepository,
      OnrUpdateService onrService,
      BatchTaskProperties taskProperties) {
    this.henkiloRepository = henkiloRepository;
    this.onrService = onrService;
    this.taskProperties = taskProperties;
  }

  @Override
  public void run(String @NonNull ... args) {
    log.info(
        "Starting ONR oppijanumero update task (batchSize={}, batchDelayMs={})",
        taskProperties.batchSize(),
        taskProperties.batchDelayMs());

    long totalUpdated = 0;
    long totalFailed = 0;
    int batchNumber = 0;
    UUID cursor = null;

    while (true) {
      // Phase 1: DB read (short transaction, connection released after)
      var batch =
          henkiloRepository.findBatchWithoutOppijanumero(taskProperties.batchSize(), cursor);
      if (batch.isEmpty()) {
        break;
      }

      // Advance cursor to last row's id for next iteration
      cursor = batch.getLast().yksiloId();

      batchNumber++;
      log.info("Processing batch {} ({} rows)", batchNumber, batch.size());

      // Phase 2: ONR API call (no transaction, no DB connection held)
      var results = onrService.processBatch(batch);
      int batchUpdated = 0;
      int batchFailed = batch.size() - results.size();

      // Phase 3: DB writes (short transaction per row)
      for (var entry : results.entrySet()) {
        try {
          henkiloRepository.updateOppijanumero(entry.getKey(), entry.getValue());
          batchUpdated++;
        } catch (Exception e) {
          log.error("Failed to update oppijanumero for yksiloId={}", entry.getKey(), e);
          batchFailed++;
        }
      }

      totalUpdated += batchUpdated;
      totalFailed += batchFailed;
      log.info(
          "Batch {} complete: updated={}, failed/skipped={}",
          batchNumber,
          batchUpdated,
          batchFailed);

      // Delay between batches to avoid overwhelming the ONR API
      if (taskProperties.batchDelayMs() > 0) {
        try {
          Thread.sleep(taskProperties.batchDelayMs());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Task interrupted during batch delay");
          break;
        }
      }
    }

    log.info(
        "ONR oppijanumero update task completed: batches={}, totalUpdated={}, totalFailed={}",
        batchNumber,
        totalUpdated,
        totalFailed);
  }
}
