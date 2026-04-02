/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.mock;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    // Generate a deterministic OID from hetu so the same person always gets the same result.
    // Use 10 digits derived from hetu hash (range 1000000000–9999999999), then append IBM check
    // digit.
    long hash = request.hetu().hashCode();
    long number = 1_000_000_000L + Math.abs(hash) % 9_000_000_000L;
    var oid = OID_PREFIX + number + luhnChecksum(number);

    log.info(
        "Resolved yleistunniste for {} ({}) {} (hetu={}...): {}",
        request.etunimet(),
        request.kutsumanimi(),
        request.sukunimi(),
        request.hetu().substring(0, Math.min(6, request.hetu().length())),
        oid);

    return ResponseEntity.ok(new HakuResult(oid, oid));
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
