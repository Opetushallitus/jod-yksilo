/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.mpassid.mock;

import static fi.okm.jod.mpassid.mock.OnrUtils.generateOid;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onr")
@Slf4j
public class OnrController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AtomicLong tuontiIdSequence = new AtomicLong(1);
  private final ConcurrentHashMap<Long, FilteredResult> tuontiResults = new ConcurrentHashMap<>();

  /**
   * Mock OAuth2 client_credentials token endpoint. Returns a dummy access token without any
   * validation.
   */
  @PostMapping("/oauth2/token")
  ResponseEntity<Map<String, Object>> token(
      @RequestParam(value = "grant_type", required = false) String grantType) {
    log.info("ONR token request, grant_type={}", grantType);
    return ResponseEntity.ok(
        Map.of(
            "access_token",
            UUID.randomUUID().toString(),
            "token_type",
            "bearer",
            "expires_in",
            3600));
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

  @PutMapping("/yleistunniste")
  ResponseEntity<OppijaTuontiPerustiedotReadDto> createYleistunniste(
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

    var rows =
        request.henkilot().stream()
            .map(
                row -> {
                  var oid = generateOid(row.henkilo().hetu());
                  log.info(
                      "PUT /onr/yleistunniste: tunniste={}, hetu={}... -> {}",
                      row.tunniste(),
                      row.henkilo().hetu().substring(0, Math.min(6, row.henkilo().hetu().length())),
                      oid);
                  return new FilteredRow(
                      row.tunniste(), new FilteredStudent(oid, oid, false), false);
                })
            .toList();

    var result = new FilteredResult(id, true, count, count, rows);
    tuontiResults.put(id, result);

    log.info("PUT /onr/yleistunniste: created tuonti id={} with {} henkilot", id, count);
    return ResponseEntity.ok(new OppijaTuontiPerustiedotReadDto(id, count, count, true));
  }

  @GetMapping("/yleistunniste/tuonti={id}")
  ResponseEntity<FilteredResult> getTuontiById(
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

    log.info(
        "GET /onr/yleistunniste/tuonti={}: returning {} henkilot", id, result.henkilot().size());
    return ResponseEntity.ok(result);
  }
}
