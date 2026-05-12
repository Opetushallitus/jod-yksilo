/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fi.okm.jod.onr.task.OnrUpdateService.TuontiResult;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.main.web-application-type=none")
class OnrUpdateTaskTest {

  @TestConfiguration
  static class TestConfig {

    @Bean
    RestClient restClient(OnrTaskProperties config, MockOnrService onrService) {
      return onrService.builder.baseUrl(config.baseUrl()).build();
    }

    @Bean
    MockOnrService mockOnrService() {
      return new MockOnrService();
    }
  }

  /**
   * Simulates the ONR yleistunniste API. Maintains an internal database of hetu-to-result mappings
   * that drives responses. The expect methods wire up HTTP expectations and delegate to service
   * methods that consult the database.
   */
  static class MockOnrService {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server =
        MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Internal database: hetu → oppijanumero mapping. */
    private final Map<String, String> database = new HashMap<>();

    /** Internal database: hetu → conflict flag. */
    private final Map<String, Boolean> conflicts = new HashMap<>();

    /** Stored submissions: tuontiId → parsed input. */
    private final Map<Long, OnrUpdateService.YleistunnisteInput> pendingTuonnit = new HashMap<>();

    private long tuontiId = 1;

    void reset() {
      database.clear();
      conflicts.clear();
      pendingTuonnit.clear();
      prepareCalls();
    }

    void verify() {
      server.verify();
    }

    // ---- Database configuration ----

    /** Registers a hetu → oppijanumero mapping in the mock database. */
    void registerOppijanumero(String hetu, String oppijanumero) {
      database.put(hetu, oppijanumero);
      conflicts.remove(hetu);
    }

    /** Registers a hetu as producing a conflict in the mock database. */
    void registerConflict(String hetu) {
      conflicts.put(hetu, true);
    }

    // ---- Service methods (simulate ONR logic) ----

    /** Stores a yleistunniste submission and returns the tuonti metadata. */
    OnrUpdateService.OppijatuontiPerustiedotDto submitYleistunniste(
        OnrUpdateService.YleistunnisteInput input, long tuontiId) {
      pendingTuonnit.put(tuontiId, input);
      return new OnrUpdateService.OppijatuontiPerustiedotDto(tuontiId);
    }

    /** Builds a tuonti result by looking up each submitted henkilo in the database. */
    TuontiResult queryTuonti(long tuontiId) {
      var input =
          requireNonNull(pendingTuonnit.get(tuontiId), "No pending tuonti for id " + tuontiId);
      var henkilot =
          input.henkilot().stream()
              .map(
                  tunniste -> {
                    var oppijanumero = database.get(tunniste.henkilo().hetu());
                    var conflict = conflicts.get(tunniste.henkilo().hetu());
                    return new TuontiResult.Tiedot(
                        tunniste.tunniste(),
                        oppijanumero != null
                            ? new TuontiResult.Oppija(oppijanumero, oppijanumero, false)
                            : null,
                        conflict != null);
                  })
              .toList();
      return new TuontiResult(tuontiId, true, henkilot);
    }

    private static final UriTemplate TUONTI_URI_TEMPLATE =
        new UriTemplate("http://localhost/onr/yleistunniste/tuonti={id}");

    /** Prepares the full yleistunniste round-trip: submission + result polling. */
    void prepareCalls() {
      server.reset();
      server
          .expect(requestTo("http://localhost/onr/yleistunniste"))
          .andExpect(method(HttpMethod.PUT))
          .andRespond(
              request -> {
                var body =
                    ((MockClientHttpRequest) requireNonNull(request, "Expected request"))
                        .getBodyAsString();
                var input = objectMapper.readValue(body, OnrUpdateService.YleistunnisteInput.class);
                var result = submitYleistunniste(input, tuontiId++);
                return withSuccess(
                        objectMapper.writeValueAsString(result), MediaType.APPLICATION_JSON)
                    .createResponse(request);
              });
      server
          .expect(request -> TUONTI_URI_TEMPLATE.matches(request.getURI().toString()))
          .andRespond(
              request -> {
                var vars = TUONTI_URI_TEMPLATE.match(request.getURI().toString());
                var id = Long.parseLong(vars.get("id"));
                var result = queryTuonti(id);
                return withSuccess(
                        objectMapper.writeValueAsString(result), MediaType.APPLICATION_JSON)
                    .createResponse(request);
              });
    }

    /** Expects PUT /yleistunniste that responds with a server error. */
    void expectServerError() {
      server.reset();
      server
          .expect(requestTo("http://localhost/onr/yleistunniste"))
          .andExpect(method(HttpMethod.PUT))
          .andRespond(withServerError());
    }
  }

  @Autowired private JdbcClient jdbc;
  @Autowired private HenkiloRepository henkiloRepository;
  @Autowired private OnrUpdateService onrUpdateService;
  @Autowired private BatchTaskProperties batchTaskProperties;
  @Autowired private MockOnrService mockOnr;

  // Valid hetus with person numbers 900-911 (9xx = test range that doesn't exist)
  // Format: 010101-NNNC where checksum C = "0123456789ABCDEFHJKLMNPRSTUVWXY"[(10101*1000+NNN) % 31]
  private static final String[] TEST_HETUS = {
    "010101-900R", "010101-901S", "010101-902T", "010101-903U", "010101-904V",
    "010101-905W", "010101-906X", "010101-907Y", "010101-9080", "010101-9091",
  };

  /** Generates a valid oppijanumero OID with correct IBM checksum. */
  private static String validOppijanumero(int index) {
    long payload = 1000000000L + index * 100L;
    int check = OppijanumeroUtils.ibmChecksum(payload);
    return "1.2.246.562.24." + payload + check;
  }

  /** Maps hetu index to a valid oppijanumero. */
  private static String validOppijanumeroForHetu(String hetu) {
    for (int i = 0; i < TEST_HETUS.length; i++) {
      if (TEST_HETUS[i].equals(hetu)) return validOppijanumero(i);
    }
    throw new IllegalArgumentException("Unknown test hetu: " + hetu);
  }

  /** Generates an invalid oppijanumero OID (wrong checksum). */
  private static String invalidOppijanumero() {
    // Use a known-bad check digit
    return "1.2.246.562.24.10000000001";
  }

  @BeforeEach
  void setUp() {
    jdbc.sql("DELETE FROM tunnistus.henkilo").update();
    mockOnr.reset();
  }

  @Test
  void processesRowsAndUpdatesOppijanumero() {
    var inserted = insertTestHenkiloBatch(10);

    // Register oppijanumero for all
    inserted.forEach(
        (hetu, yksiloId) -> mockOnr.registerOppijanumero(hetu, validOppijanumeroForHetu(hetu)));

    new Application(henkiloRepository, onrUpdateService, batchTaskProperties).run();
    mockOnr.verify();

    inserted.forEach(
        (hetu, yksiloId) -> {
          var stored =
              jdbc.sql("SELECT oppijanumero FROM tunnistus.henkilo WHERE yksilo_id = :id")
                  .param("id", yksiloId)
                  .query((rs, _) -> rs.getString("oppijanumero"))
                  .single();
          assertThat(stored).isEqualTo("ONR:" + validOppijanumeroForHetu(hetu));
        });
  }

  @Test
  void doesNotUpdateOppijanumeroWhenOnrReturnsNoMatches() {
    var inserted = insertTestHenkiloBatch(10);

    // Don't register any oppijanumeros - ONR returns no matches
    new Application(henkiloRepository, onrUpdateService, batchTaskProperties).run();
    mockOnr.verify();

    inserted.forEach(
        (hetu, yksiloId) -> {
          var row =
              jdbc.sql("SELECT oppijanumero FROM tunnistus.henkilo WHERE yksilo_id = :id")
                  .param("id", yksiloId)
                  .query((rs, _) -> rs.getString("oppijanumero"))
                  .optional()
                  .orElse(null);
          assertThat(row).isNull();
        });
  }

  @Test
  void updatesOppijanumeroWhenOnrReturnsValidResult() {
    var inserted = insertTestHenkiloBatch(10);

    inserted.forEach(
        (hetu, yksiloId) -> mockOnr.registerOppijanumero(hetu, validOppijanumeroForHetu(hetu)));

    var batch = henkiloRepository.findBatchWithoutOppijanumero(100, null);
    var results = onrUpdateService.processBatch(batch);
    mockOnr.verify();

    assertThat(results).hasSize(10);

    // Apply updates and verify
    inserted.forEach(
        (hetu, yksiloId) -> {
          var expected = "ONR:" + validOppijanumeroForHetu(hetu);
          assertThat(results.get(yksiloId)).isEqualTo(expected);
          henkiloRepository.updateOppijanumero(yksiloId, results.get(yksiloId));
        });

    // Verify DB
    inserted.forEach(
        (hetu, yksiloId) -> {
          var stored =
              jdbc.sql("SELECT oppijanumero FROM tunnistus.henkilo WHERE yksilo_id = :id")
                  .param("id", yksiloId)
                  .query((rs, _) -> rs.getString("oppijanumero"))
                  .single();
          assertThat(stored).isEqualTo("ONR:" + validOppijanumeroForHetu(hetu));
        });
  }

  @Test
  void skipsConflictingOnrResults() {
    var inserted = insertTestHenkiloBatch(10);
    var hetus = List.copyOf(inserted.keySet());
    var conflicting = hetus.subList(0, 5);
    var valid = hetus.subList(5, 10);

    conflicting.forEach(mockOnr::registerConflict);
    valid.forEach(hetu -> mockOnr.registerOppijanumero(hetu, validOppijanumeroForHetu(hetu)));

    var batch =
        henkiloRepository.findBatchWithoutOppijanumero(batchTaskProperties.batchSize(), null);
    var results = onrUpdateService.processBatch(batch);
    mockOnr.verify();

    assertThat(results).hasSize(5);
    conflicting.forEach(hetu -> assertThat(results).doesNotContainKey(inserted.get(hetu)));
    valid.forEach(
        hetu ->
            assertThat(results.get(inserted.get(hetu)))
                .isEqualTo("ONR:" + validOppijanumeroForHetu(hetu)));
  }

  @Test
  void skipsInvalidOppijanumeroFromOnr() {
    var inserted = insertTestHenkiloBatch(10);
    var hetus = List.copyOf(inserted.keySet());
    var invalid = hetus.subList(0, 5);
    var valid = hetus.subList(5, 10);

    invalid.forEach(hetu -> mockOnr.registerOppijanumero(hetu, invalidOppijanumero()));
    valid.forEach(hetu -> mockOnr.registerOppijanumero(hetu, validOppijanumeroForHetu(hetu)));

    var batch =
        henkiloRepository.findBatchWithoutOppijanumero(batchTaskProperties.batchSize(), null);
    var results = onrUpdateService.processBatch(batch);
    mockOnr.verify();

    assertThat(results).hasSize(5);
    invalid.forEach(hetu -> assertThat(results).doesNotContainKey(inserted.get(hetu)));
    valid.forEach(
        hetu ->
            assertThat(results.get(inserted.get(hetu)))
                .isEqualTo("ONR:" + validOppijanumeroForHetu(hetu)));
  }

  @Test
  void skipsRowsWithExistingOppijanumero() {
    // Insert 10 rows, all with existing oppijanumero
    for (int i = 0; i < 10; i++) {
      var yksiloId = UUID.randomUUID();
      jdbc.sql(
              """
              INSERT INTO tunnistus.henkilo (yksilo_id, henkilo_id, oppijanumero, etunimi, sukunimi)
              VALUES (:id, :henkiloId, :oppijanumero, 'Testi', 'Käyttäjä')
              """)
          .param("id", yksiloId)
          .param("henkiloId", "FIN:" + TEST_HETUS[i])
          .param("oppijanumero", "ONR:" + validOppijanumero(i))
          .update();
    }

    var batch = henkiloRepository.findBatchWithoutOppijanumero(100, null);
    assertThat(batch).isEmpty();
  }

  @Test
  void cursorPaginationWorks() {
    var id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var id3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    for (var id : List.of(id1, id2, id3)) {
      jdbc.sql(
              """
              INSERT INTO tunnistus.henkilo (yksilo_id, henkilo_id, etunimi, sukunimi)
              VALUES (:id, 'FIN:010101-' || REPLACE(CAST(:id AS VARCHAR), '-', ''), 'Test', 'User')
              """)
          .param("id", id)
          .update();
    }

    var firstBatch = henkiloRepository.findBatchWithoutOppijanumero(2, null);
    assertThat(firstBatch).hasSize(2);
    assertThat(firstBatch.get(0).yksiloId()).isEqualTo(id1);
    assertThat(firstBatch.get(1).yksiloId()).isEqualTo(id2);

    var secondBatch = henkiloRepository.findBatchWithoutOppijanumero(2, id2);
    assertThat(secondBatch).hasSize(1);
    assertThat(secondBatch.getFirst().yksiloId()).isEqualTo(id3);
  }

  @Test
  void handleOnrApiFailure() {
    insertTestHenkiloBatch(10);

    mockOnr.expectServerError();

    var batch = henkiloRepository.findBatchWithoutOppijanumero(100, null);
    var results = onrUpdateService.processBatch(batch);
    mockOnr.verify();

    assertThat(results).isEmpty();
  }

  /**
   * Inserts a batch of test henkilö rows with valid Finnish hetus. Returns a map of hetu → yksiloId
   * (insertion-ordered) that does not depend on any DB ordering.
   */
  private Map<String, UUID> insertTestHenkiloBatch(int count) {
    var result = new LinkedHashMap<String, UUID>();
    for (int i = 0; i < count; i++) {
      var yksiloId = UUID.randomUUID();
      jdbc.sql(
              """
              INSERT INTO tunnistus.henkilo (yksilo_id, henkilo_id, oppijanumero, etunimi, sukunimi)
              VALUES (:id, :henkiloId, NULL, :etunimi, :sukunimi)
              """)
          .param("id", yksiloId)
          .param("henkiloId", "FIN:" + TEST_HETUS[i])
          .param("etunimi", "Testi" + i)
          .param("sukunimi", "Käyttäjä" + i)
          .update();
      result.put(TEST_HETUS[i], yksiloId);
    }
    return result;
  }
}
