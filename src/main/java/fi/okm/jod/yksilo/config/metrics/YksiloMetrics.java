/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.metrics;

import fi.okm.jod.yksilo.repository.YksiloRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
class YksiloMetrics {

  private final MeterRegistry meterRegistry;
  private final YksiloRepository yksiloRepository;
  private final AtomicLong yksiloCount = new AtomicLong(0);

  @PostConstruct
  void init() {
    Gauge.builder("fi.okm.jod.yksilo.count", yksiloCount, AtomicLong::doubleValue)
        .description("Number of users in the system")
        .register(meterRegistry);
  }

  @Scheduled(fixedDelay = 1, initialDelay = 0, timeUnit = TimeUnit.HOURS)
  @Transactional(readOnly = true)
  void updateYksiloCount() {
    var count = yksiloRepository.count();
    yksiloCount.set(count);
    log.debug("Updated yksilo count metric: {}", count);
  }
}
