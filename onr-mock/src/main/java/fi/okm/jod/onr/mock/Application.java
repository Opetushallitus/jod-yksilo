/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Slf4j
public class Application {

  private static final String OID_PREFIX = "1.2.246.562.98.";
  private static final String BEARER_PREFIX = "Bearer ";

  private final AtomicLong tuontiIdSequence = new AtomicLong(1);
  private final ConcurrentHashMap<Long, FilteredResult> tuontiResults = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    log.info("Starting ONR Mock API");
    SpringApplication.run(Application.class, args);
  }

  /**
   * Mock OAuth2 client_credentials token endpoint. Returns a dummy access token without any
   * validation.
   */
  @PostMapping("/oauth2/token")
  ResponseEntity<Map<String, Object>> token(
      @RequestParam(value = "grant_type", required = false) String grantType) {
    log.info("Token request, grant_type={}", grantType);
    return ResponseEntity.ok(
        Map.of(
            "access_token",
            UUID.randomUUID().toString(),
            "token_type",
            "bearer",
            "expires_in",
            3600));
  }

  // -- DTOs for POST /yleistunniste/hae --

  record TunnesteHakuDto(String etunimet, String kutsumanimi, String sukunimi, String hetu) {}

  record HakuResult(String oid, String oppijanumero) {}

  /**
   * Mock implementation of ONR yleistunniste/hae endpoint. Returns a deterministic OID derived from
   * the hetu so the same person always gets the same oppijanumero.
   */
  @PostMapping("/yleistunniste/hae")
  ResponseEntity<HakuResult> yleistunnisteenHaku(
      @RequestHeader("Authorization") String auth, @RequestBody TunnesteHakuDto request) {
    if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Bearer token required"))
          .build();
    }

    var missing = missingField(request);
    if (missing != null) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, missing + " is required"))
          .build();
    }

    var oid = generateOid(request.hetu());

    log.info(
        "Resolved yleistunniste for {} ({}) {} (hetu={}...): {}",
        request.etunimet(),
        request.kutsumanimi(),
        request.sukunimi(),
        request.hetu().substring(0, Math.min(6, request.hetu().length())),
        oid);

    return ResponseEntity.ok(new HakuResult(oid, oid));
  }

  // -- DTOs for PUT /yleistunniste (batch creation) --

  record YleistunnisteInput(String sahkoposti, List<YleistunnisteInputRow> henkilot) {}

  record YleistunnisteInputRow(String tunniste, HenkiloInput henkilo) {}

  record HenkiloInput(String etunimet, String kutsumanimi, String sukunimi, String hetu) {}

  record OppijaTuontiPerustiedotReadDto(
      long id, int kasiteltavia, int kasiteltyja, boolean kasitelty) {}

  // -- DTOs for GET /yleistunniste/tuonti={id} --

  record FilteredResult(
      long id, boolean kasitelty, int kasiteltavia, int kasiteltyja, List<FilteredRow> henkilot) {}

  record FilteredRow(String tunniste, FilteredStudent henkilo, boolean conflict) {}

  record FilteredStudent(String oid, String oppijanumero, boolean passivoitu) {}

  /**
   * Mock implementation of ONR PUT /yleistunniste endpoint. Processes the batch synchronously and
   * stores the result for retrieval via GET /yleistunniste/tuonti={id}.
   */
  @PutMapping("/yleistunniste")
  ResponseEntity<?> createYleistunniste(
      @RequestHeader("Authorization") String auth, @RequestBody YleistunnisteInput request) {

    if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Bearer token required"))
          .build();
    }

    if (request.henkilot() == null || request.henkilot().isEmpty()) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "henkilot is required"))
          .build();
    }

    var id = tuontiIdSequence.getAndIncrement();
    var count = request.henkilot().size();

    // Process each person and generate deterministic OIDs
    var rows =
        request.henkilot().stream()
            .map(
                row -> {
                  var oid = generateOid(row.henkilo().hetu());
                  log.info(
                      "PUT /yleistunniste: tunniste={}, hetu={}... -> {}",
                      row.tunniste(),
                      row.henkilo().hetu().substring(0, Math.min(6, row.henkilo().hetu().length())),
                      oid);
                  return new FilteredRow(
                      row.tunniste(), new FilteredStudent(oid, oid, false), false);
                })
            .toList();

    var result = new FilteredResult(id, true, count, count, rows);
    tuontiResults.put(id, result);

    log.info("PUT /yleistunniste: created tuonti id={} with {} henkilot", id, count);
    return ResponseEntity.ok(new OppijaTuontiPerustiedotReadDto(id, count, count, true));
  }

  /**
   * Mock implementation of ONR GET /yleistunniste/tuonti={id} endpoint. Returns the pre-computed
   * results from the corresponding PUT request.
   */
  @GetMapping("/yleistunniste/tuonti={id}")
  ResponseEntity<?> getTuontiById(
      @RequestHeader("Authorization") String auth, @PathVariable("id") long id) {

    if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Bearer token required"))
          .build();
    }

    var result = tuontiResults.get(id);
    if (result == null) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Tuonti not found: " + id))
          .build();
    }

    log.info("GET /yleistunniste/tuonti={}: returning {} henkilot", id, result.henkilot().size());
    return ResponseEntity.ok(result);
  }

  // -- Shared helpers --

  /** Generates a deterministic OID from hetu using hash + Luhn checksum. */
  private static String generateOid(String hetu) {
    long hash = hetu.hashCode();
    long number = 1_000_000_000L + Math.abs(hash) % 9_000_000_000L;
    return OID_PREFIX + number + luhnChecksum(number);
  }

  private static String missingField(TunnesteHakuDto request) {
    if (request.hetu() == null || request.hetu().isBlank()) {
      return "hetu";
    }
    if (request.etunimet() == null || request.etunimet().isBlank()) {
      return "etunimet";
    }
    if (request.kutsumanimi() == null || request.kutsumanimi().isBlank()) {
      return "kutsumanimi";
    }
    if (request.sukunimi() == null || request.sukunimi().isBlank()) {
      return "sukunimi";
    }
    return null;
  }

  /** Computes the Luhn checksum for the given payload. */
  static int luhnChecksum(long payload) {
    final int[] weights = {2, 1};
    int sum = 0;
    for (int j = 0; payload > 0; payload /= 10, j++) {
      int n = (int) (payload % 10) * weights[j % 2];
      if (n > 9) {
        n -= 9;
      }
      sum += n;
    }
    return (10 - sum % 10) % 10;
  }
}
