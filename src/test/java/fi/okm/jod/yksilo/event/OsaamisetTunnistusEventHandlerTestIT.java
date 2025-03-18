/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import fi.okm.jod.yksilo.testutil.TestJodUser;
import fi.okm.jod.yksilo.testutil.TestUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@TestPropertySource(
    properties = "jod.ai-tunnistus.osaamiset.endpoint=http://mylocalhost/invocations")
@Sql(value = "/data/osaaminen.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class OsaamisetTunnistusEventHandlerTestIT implements IntegrationTest {

  private static final String ENDPOINT_URL = "http://mylocalhost/invocations";

  @Container @ServiceConnection
  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
      TestUtil.createPostgresSQLContainer();

  @Autowired private OsaamisetTunnistusEventHandler eventHandler;
  @Autowired private PlatformTransactionManager transactionManager;

  @PersistenceContext private EntityManager entityManager;

  @MockitoBean
  private InferenceService<
          OsaamisetTunnistusEventHandler.SageMakerRequest,
          OsaamisetTunnistusEventHandler.SageMakerResponse>
      inferenceService;

  private TransactionTemplate transactionTemplate;
  private TestJodUser user;
  private List<Koulutus> koulutukset;

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(
        status -> {
          var yksiloUser = new Yksilo(UUID.randomUUID());
          entityManager.persist(yksiloUser);
          user = new TestJodUser(yksiloUser.getId());

          var yksilo = entityManager.find(Yksilo.class, user.getId());
          var kokonaisuus = createKoulutusKokonaisuus(yksilo);

          var koulutus1 = new Koulutus(kokonaisuus);
          koulutus1.setNimi(new LocalizedString(Map.of(Kieli.FI, "Koulutus 1")));
          koulutus1.setAlkuPvm(LocalDate.of(2023, 1, 1));
          koulutus1.setLoppuPvm(LocalDate.of(2023, 12, 31));
          koulutus1.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);
          entityManager.persist(koulutus1);

          var koulutus2 = new Koulutus(kokonaisuus);
          koulutus2.setNimi(new LocalizedString(Map.of(Kieli.FI, "Koulutus 2")));
          koulutus2.setAlkuPvm(LocalDate.of(2025, 1, 1));
          koulutus2.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);
          entityManager.persist(koulutus2);

          koulutukset = Arrays.asList(koulutus1, koulutus2);

          return null;
        });
  }

  private KoulutusKokonaisuus createKoulutusKokonaisuus(Yksilo yksilo) {
    var kokonaisuus =
        new KoulutusKokonaisuus(yksilo, new LocalizedString(Map.of(Kieli.FI, "Test Kokonaisuus")));
    entityManager.persist(kokonaisuus);
    return kokonaisuus;
  }

  @Test
  void shouldSuccessfullyProcessOsaamisetTunnistusEvent()
      throws ExecutionException, InterruptedException, TimeoutException {
    var osaaminen1 = "urn:osaaminen1";
    var osaaminen2 = "urn:osaaminen2";

    var koulutus1 = koulutukset.getFirst();
    var osasuoritukset =
        Set.of(
            "Kasvu, kehitys ja oppiminen (AVOIN YO)",
            "Opetus ja kasvatuksellinen vuorovaikutus (AVOIN YO)");
    koulutus1.setOsasuoritukset(osasuoritukset);

    var sageMakerRequest =
        new OsaamisetTunnistusEventHandler.SageMakerRequest(
            koulutus1.getId(), koulutus1.getNimi().get(Kieli.FI), osasuoritukset);

    var sageMakerResponse =
        new OsaamisetTunnistusEventHandler.SageMakerResponse(
            koulutus1.getId(), Set.of(URI.create(osaaminen1), URI.create(osaaminen2)));

    when(inferenceService.infer(
            ENDPOINT_URL, sageMakerRequest, new ParameterizedTypeReference<>() {}))
        .thenReturn(sageMakerResponse);

    var result =
        eventHandler.handleOsaamisetTunnistusEvent(
            new OsaamisetTunnistusEvent(user, List.of(koulutus1)));
    result.get(5, TimeUnit.SECONDS); // Wait for execution to be completed.

    verifyOsaamisetUpdated(koulutus1, sageMakerResponse.osaamiset());
    verify(inferenceService)
        .infer(ENDPOINT_URL, sageMakerRequest, new ParameterizedTypeReference<>() {});
  }

  private void verifyOsaamisetUpdated(Koulutus koulutus, Set<URI> expectedOsaamisetUris) {
    transactionTemplate.execute(
        status -> {
          var updatedKoulutus = entityManager.find(Koulutus.class, koulutus.getId());

          assertNotNull(updatedKoulutus, "Updated koulutus should not be null");
          assertEquals(
              OsaamisenTunnistusStatus.DONE, updatedKoulutus.getOsaamisenTunnistusStatus());

          var actualUris =
              updatedKoulutus.getOsaamiset().stream()
                  .map(o -> o.getOsaaminen().getUri())
                  .collect(Collectors.toSet());

          assertNotNull(
              expectedOsaamisetUris,
              "Expected URIs should exist for koulutus ID: " + koulutus.getId());
          assertTrue(
              actualUris.containsAll(expectedOsaamisetUris),
              "Osaamiset URIs should match for koulutus ID: " + koulutus.getId());
          return null;
        });
  }

  @Test
  void shouldHandleApiErrorsGracefully()
      throws ExecutionException, InterruptedException, TimeoutException {
    when(inferenceService.infer(
            eq(ENDPOINT_URL),
            any(OsaamisetTunnistusEventHandler.SageMakerRequest.class),
            eq(
                new ParameterizedTypeReference<
                    OsaamisetTunnistusEventHandler.SageMakerResponse>() {})))
        .thenThrow(new RuntimeException("Internal Server error"));

    var result =
        eventHandler.handleOsaamisetTunnistusEvent(new OsaamisetTunnistusEvent(user, koulutukset));
    result.get(5, TimeUnit.SECONDS); // Wait for execution to be completed.

    verify(inferenceService, times(koulutukset.size()))
        .infer(
            eq(ENDPOINT_URL),
            any(OsaamisetTunnistusEventHandler.SageMakerRequest.class),
            eq(
                new ParameterizedTypeReference<
                    OsaamisetTunnistusEventHandler.SageMakerResponse>() {}));
    verifyKoulutusStatuses(koulutukset, OsaamisenTunnistusStatus.FAIL);
  }

  private void verifyKoulutusStatuses(
      List<Koulutus> koulutukset, OsaamisenTunnistusStatus expectedStatus) {
    transactionTemplate.execute(
        status -> {
          for (Koulutus koulutus : koulutukset) {
            var updatedKoulutus = entityManager.find(Koulutus.class, koulutus.getId());
            assertNotNull(updatedKoulutus, "Koulutus should exist");
            assertEquals(
                expectedStatus,
                updatedKoulutus.getOsaamisenTunnistusStatus(),
                "Koulutus should have status: " + expectedStatus);
          }
          return null;
        });
  }
}
