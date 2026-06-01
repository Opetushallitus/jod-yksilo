/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fi.okm.jod.yksilo.config.CvProperties;
import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.entity.CvTehtava;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.CvTehtavaRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.ServiceConflictException;
import fi.okm.jod.yksilo.service.ServiceOverloadedException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration test for the rate-limit logic in {@link CvService}. */
class CvServiceRateLimitTest extends AbstractServiceTest {

  private static final int RATE_LIMIT = 3;

  /** No-op storage; the rate-limit logic does not perform any uploads. */
  private static final CvStorage NOOP_STORAGE = (_, _, _) -> "noop-key";

  /** No-op message sender; the rate-limit logic does not enqueue messages. */
  private static final CvMessageSender NOOP_SENDER = _ -> {};

  @Autowired private CvTehtavaRepository tehtavat;

  private CvService service;

  @BeforeEach
  void initService() {
    var properties = new CvProperties(null, "jod-cv", 2_097_152, RATE_LIMIT, null, null);
    service =
        new CvService(
            tehtavat,
            yksiloRepository,
            NOOP_STORAGE,
            NOOP_SENDER,
            // The remaining collaborators are not used by checkRateLimit.
            null,
            null,
            null,
            properties);
  }

  @Test
  void shouldPassWhenNoTasksExist() {
    assertThatCode(() -> service.checkRateLimit(user)).doesNotThrowAnyException();
  }

  @Test
  void shouldPassWhenBelowLimitAndNoInflight() {
    persist(user.getId(), CvTehtavaTila.VALMIS);
    persist(user.getId(), CvTehtavaTila.EPAONNISTUNUT);
    simulateCommit();

    assertThatCode(() -> service.checkRateLimit(user)).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWhenInflightTaskExists() {
    // A single pending (ODOTTAA) task must trigger a conflict, even though the total
    // count is well below the daily rate limit.
    persist(user.getId(), CvTehtavaTila.ODOTTAA);
    simulateCommit();

    assertThatThrownBy(() -> service.checkRateLimit(user))
        .isInstanceOf(ServiceConflictException.class)
        .hasMessageContaining("In-flight");
  }

  @Test
  void shouldPassWhenExactlyAtLimit() {
    // The check only fails when the count strictly exceeds rateLimit.
    for (int i = 0; i < RATE_LIMIT; i++) {
      persist(user.getId(), CvTehtavaTila.VALMIS);
    }
    simulateCommit();

    assertThatCode(() -> service.checkRateLimit(user)).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWhenRateLimitExceeded() {
    // No in-flight tasks, but the total over the past day exceeds the configured limit.
    for (int i = 0; i < RATE_LIMIT + 1; i++) {
      persist(user.getId(), CvTehtavaTila.VALMIS);
    }
    simulateCommit();

    assertThatThrownBy(() -> service.checkRateLimit(user))
        .isInstanceOf(ServiceOverloadedException.class)
        .hasMessageContaining("rate limit");
  }

  @Test
  void shouldIgnoreTasksOlderThanOneDay() {
    // Insert more than the rate-limit, but back-date them beyond the 24h window so
    // they fall out of the custom query's filter.
    for (int i = 0; i < RATE_LIMIT + 5; i++) {
      var tehtava = persist(user.getId(), CvTehtavaTila.VALMIS);
      backdate(tehtava.getId(), Instant.now().minus(2, ChronoUnit.DAYS));
    }
    simulateCommit();

    assertThatCode(() -> service.checkRateLimit(user)).doesNotThrowAnyException();
  }

  private CvTehtava persist(UUID yksiloId, CvTehtavaTila tila) {
    var yksilo = entityManager.getEntityManager().getReference(Yksilo.class, yksiloId);
    var tehtava = new CvTehtava(yksilo, Kieli.FI);
    tehtava.setTila(tila);
    return entityManager.persist(tehtava);
  }

  private void backdate(UUID id, Instant luotu) {
    entityManager
        .getEntityManager()
        .createNativeQuery("UPDATE cv_tehtava SET luotu = ?1 WHERE id = ?2")
        .setParameter(1, luotu)
        .setParameter(2, id)
        .executeUpdate();
  }
}
