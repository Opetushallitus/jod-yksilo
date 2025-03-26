/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.AbstractIntegrationTest;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Yksilo;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

class OsaamisetTunnistusEventHandlerTestIT implements AbstractIntegrationTest {

  @Container @ServiceConnection
  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
      TestUtil.createPostgresSQLContainer();

  @Container @ServiceConnection
  private static final GenericContainer<?> REDIS_CONTAINER = TestUtil.createRedisContainer();

  @Container
  private static final MockServerContainer MOCK_SERVER_CONTAINER =
      TestUtil.createMockServerContainer();

  private static final String URL_PATH = "/api/tunnistus/osaamiset";

  private static MockServerClient mockServerClient;

  @Autowired private OsaamisetTunnistusEventHandler eventHandler;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PlatformTransactionManager transactionManager;

  @PersistenceContext private EntityManager entityManager;

  private TransactionTemplate transactionTemplate;
  private TestJodUser user;
  private List<Koulutus> koulutukset;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "jod.ai-tunnistus.osaamiset.url",
        () ->
            "http://"
                + MOCK_SERVER_CONTAINER.getHost()
                + ":"
                + MOCK_SERVER_CONTAINER.getServerPort()
                + URL_PATH);
  }

  @BeforeAll
  static void setupMockServer() {
    mockServerClient =
        new MockServerClient(
            MOCK_SERVER_CONTAINER.getHost(), MOCK_SERVER_CONTAINER.getServerPort());
  }

  @BeforeEach
  void setUp() {
    mockServerClient.reset();

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

          entityManager.createNativeQuery("DELETE FROM osaaminen").executeUpdate();
          entityManager.flush();
          entityManager
              .createNativeQuery(
                  """
            INSERT INTO osaaminen(uri)
            VALUES ('urn:osaaminen1'),
                   ('urn:osaaminen2'),
                   ('urn:osaaminen3')
            """)
              .executeUpdate();

          return null;
        });
  }

  private KoulutusKokonaisuus createKoulutusKokonaisuus(Yksilo yksilo) {
    var kokonaisuus =
        new KoulutusKokonaisuus(yksilo, new LocalizedString(Map.of(Kieli.FI, "Test Kokonaisuus")));
    entityManager.persist(kokonaisuus);
    return kokonaisuus;
  }

  @AfterEach
  void tearDown() {
    mockServerClient.reset();
  }

  @Test
  void shouldSuccessfullyProcessOsaamisetTunnistusEvent() throws Exception {
    var osaaminen1 = "urn:osaaminen1";
    var osaaminen2 = "urn:osaaminen2";
    var osaaminen3 = "urn:osaaminen3";

    Map<UUID, Set<String>> koulutusToOsaamisetMap =
        Map.of(
            koulutukset.get(0).getId(), Set.of(osaaminen1, osaaminen2),
            koulutukset.get(1).getId(), Set.of(osaaminen3));

    List<OsaamisetTunnistusEventHandler.OsaamisetTunnistusResponse> apiResponseList =
        koulutusToOsaamisetMap.entrySet().stream()
            .map(
                entry ->
                    new OsaamisetTunnistusEventHandler.OsaamisetTunnistusResponse(
                        entry.getKey(),
                        entry.getValue().stream().map(URI::create).collect(Collectors.toSet())))
            .toList();

    mockServerClient
        .when(HttpRequest.request().withMethod(HttpMethod.POST.name()).withPath(URL_PATH))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(apiResponseList)));

    eventHandler.handleOsaamisetTunnistusEvent(new OsaamisetTunnistusEvent(user, koulutukset));

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> verifyOsaamisetUpdated(koulutusToOsaamisetMap));

    mockServerClient.verify(
        HttpRequest.request().withMethod(HttpMethod.POST.name()).withPath(URL_PATH),
        VerificationTimes.exactly(1));
  }

  private void verifyOsaamisetUpdated(Map<UUID, Set<String>> koulutusToOsaamisetMap) {
    transactionTemplate.execute(
        status -> {
          koulutukset.forEach(
              koulutus -> {
                var updatedKoulutus = entityManager.find(Koulutus.class, koulutus.getId());

                assertNotNull(updatedKoulutus, "Updated koulutus should not be null");
                assertEquals(
                    OsaamisenTunnistusStatus.DONE, updatedKoulutus.getOsaamisenTunnistusStatus());

                var actualUris =
                    updatedKoulutus.getOsaamiset().stream()
                        .map(o -> o.getOsaaminen().getUri())
                        .collect(Collectors.toSet());

                Set<String> expectedUris = koulutusToOsaamisetMap.get(koulutus.getId());
                assertNotNull(
                    expectedUris,
                    "Expected URIs should exist for koulutus ID: " + koulutus.getId());
                assertEquals(
                    expectedUris,
                    actualUris,
                    "Osaamiset URIs should match for koulutus ID: " + koulutus.getId());
              });
          return null;
        });
  }

  @Test
  void shouldHandleApiErrorsGracefully() {
    mockServerClient
        .when(HttpRequest.request().withMethod(HttpMethod.POST.name()).withPath(URL_PATH))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    eventHandler.handleOsaamisetTunnistusEvent(new OsaamisetTunnistusEvent(user, koulutukset));

    // Since the event processing is @Async, we need to wait for it to complete.
    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> verifyKoulutusStatuses(OsaamisenTunnistusStatus.FAIL));

    mockServerClient.verify(
        HttpRequest.request().withMethod(HttpMethod.POST.name()).withPath(URL_PATH),
        VerificationTimes.exactly(1));
  }

  private void verifyKoulutusStatuses(OsaamisenTunnistusStatus expectedStatus) {
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
