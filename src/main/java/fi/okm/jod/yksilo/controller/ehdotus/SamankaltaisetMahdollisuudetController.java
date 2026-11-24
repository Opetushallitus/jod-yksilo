/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.dto.ArvoDto;
import fi.okm.jod.yksilo.dto.JakaumaDto;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.Request;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Finds opportunities similar to a given opportunity using the kohtaanto inference endpoint.
 *
 * <p>The scores are relative to the request, so a fixed score limit would not work. Instead, the
 * best matches of each type are returned.
 *
 * <p>The suggestions are always searched in Finnish, so they are the same in all the languages of
 * the UI. The result depends only on the opportunity, never on the user or the language, so the
 * responses are cacheable in shared caches (CloudFront) by the path alone.
 */
@RestController
@Slf4j
class SamankaltaisetMahdollisuudetController {

  /** Language of the description, the inference endpoint and the opportunities used. */
  static final Kieli KIELI = Kieli.FI;

  /** Number of the best matching opportunities returned for each type. */
  static final int RESULTS_PER_TYPE = 6;

  // Limits of the request validation of the POST /api/ehdotus/mahdollisuudet endpoint
  private static final int MAX_OSAAMISET = 1000;
  private static final int MAX_FREE_TEXT_LENGTH = 10000;

  /** How long browsers may reuse the response. */
  static final Duration BROWSER_MAX_AGE = Duration.ofHours(1);

  /**
   * How long shared caches (CloudFront) may reuse the response. The suggestions only change when
   * the opportunity data or the kohtaanto model is updated.
   */
  static final Duration SHARED_MAX_AGE = Duration.ofDays(1);

  private final MahdollisuusEhdotusService mahdollisuusEhdotusService;
  private final MahdollisuudetService mahdollisuudetService;
  private final TyomahdollisuusService tyomahdollisuusService;
  private final KoulutusmahdollisuusService koulutusmahdollisuusService;

  SamankaltaisetMahdollisuudetController(
      MahdollisuusEhdotusService mahdollisuusEhdotusService,
      MahdollisuudetService mahdollisuudetService,
      TyomahdollisuusService tyomahdollisuusService,
      KoulutusmahdollisuusService koulutusmahdollisuusService) {
    this.mahdollisuusEhdotusService = mahdollisuusEhdotusService;
    this.mahdollisuudetService = mahdollisuudetService;
    this.tyomahdollisuusService = tyomahdollisuusService;
    this.koulutusmahdollisuusService = koulutusmahdollisuusService;
  }

  /**
   * A similar opportunity.
   *
   * @param id Opportunity ID
   * @param tyyppi Type of the opportunity
   * @param pisteet Similarity score (0-1), where 1 is the best match
   */
  public record SamankaltainenMahdollisuusDto(
      @NotNull UUID id, @NotNull MahdollisuusTyyppi tyyppi, @NotNull Double pisteet) {}

  @GetMapping("/api/tyomahdollisuudet/{id}/samankaltaiset")
  @Tag(name = "tyomahdollisuudet")
  @Operation(
      summary = "Get opportunities similar to the työmahdollisuus",
      description =
          """
          Returns the six best matching työmahdollisuudet and koulutusmahdollisuudet,
          työmahdollisuudet first, each type ordered by the score. The response is cacheable.
          """)
  public ResponseEntity<List<SamankaltainenMahdollisuusDto>> findSimilarToTyomahdollisuus(
      @PathVariable UUID id) {
    var tyomahdollisuus = tyomahdollisuusService.get(id);
    return respond(
        id,
        MahdollisuusTyyppi.TYOMAHDOLLISUUS,
        osaamiset(tyomahdollisuus.jakaumat(), TyomahdollisuusJakaumaTyyppi.OSAAMINEN),
        tyomahdollisuus.kuvaus());
  }

  @GetMapping("/api/koulutusmahdollisuudet/{id}/samankaltaiset")
  @Tag(name = "koulutusmahdollisuudet")
  @Operation(
      summary = "Get opportunities similar to the koulutusmahdollisuus",
      description =
          """
          Returns the six best matching koulutusmahdollisuudet and työmahdollisuudet,
          koulutusmahdollisuudet first, each type ordered by the score. The response is cacheable.
          """)
  public ResponseEntity<List<SamankaltainenMahdollisuusDto>> findSimilarToKoulutusmahdollisuus(
      @PathVariable UUID id) {
    var koulutusmahdollisuus = koulutusmahdollisuusService.get(id);
    return respond(
        id,
        MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS,
        osaamiset(koulutusmahdollisuus.jakaumat(), KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN),
        koulutusmahdollisuus.kuvaus());
  }

  private ResponseEntity<List<SamankaltainenMahdollisuusDto>> respond(
      UUID lahdeId, MahdollisuusTyyppi lahdeTyyppi, Set<URI> osaamiset, LocalizedString kuvaus) {
    log.info("Finding opportunities similar to {} {}", lahdeTyyppi, lahdeId);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(BROWSER_MAX_AGE).sMaxAge(SHARED_MAX_AGE).cachePublic())
        .body(findSimilar(lahdeId, lahdeTyyppi, osaamiset, kuvaus));
  }

  private List<SamankaltainenMahdollisuusDto> findSimilar(
      UUID lahdeId,
      MahdollisuusTyyppi lahdeTyyppi,
      Set<URI> jakaumaOsaamiset,
      LocalizedString kuvaus) {
    var osaamiset = mahdollisuusEhdotusService.knownOsaamiset(jakaumaOsaamiset);
    var vapaateksti = truncate(localized(kuvaus).trim(), MAX_FREE_TEXT_LENGTH);

    // Without any input the suggestions are not ranked, so there is nothing meaningful to return
    if (osaamiset.isEmpty() && vapaateksti.isEmpty()) {
      return List.of();
    }

    // Same weights as used by the opportunity pages before this endpoint existed
    var suggestions =
        mahdollisuusEhdotusService.infer(
            KIELI,
            new Request.Data(osaamiset, vapaateksti, 1, Set.of(), null, 0, 0.5, 0.5, Set.of()));

    // Only the active opportunities known by the backend are returned
    var known =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, KIELI);

    var ranked =
        suggestions.values().stream()
            .filter(s -> !s.id().equals(lahdeId) && known.containsKey(s.id()))
            .map(
                s ->
                    new SamankaltainenMahdollisuusDto(
                        s.id(), known.get(s.id()).tyyppi(), s.score()))
            .sorted(Comparator.comparingDouble(SamankaltainenMahdollisuusDto::pisteet).reversed())
            .toList();

    // The scores of the different types are not comparable, so the types are not mixed
    var orderedTypes =
        lahdeTyyppi == MahdollisuusTyyppi.TYOMAHDOLLISUUS
            ? List.of(MahdollisuusTyyppi.TYOMAHDOLLISUUS, MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
            : List.of(MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS, MahdollisuusTyyppi.TYOMAHDOLLISUUS);
    return orderedTypes.stream()
        .flatMap(
            tyyppi -> ranked.stream().filter(s -> s.tyyppi() == tyyppi).limit(RESULTS_PER_TYPE))
        .toList();
  }

  private static <K> Set<URI> osaamiset(Map<K, JakaumaDto> jakaumat, K osaaminen) {
    var jakauma = jakaumat == null ? null : jakaumat.get(osaaminen);
    if (jakauma == null || jakauma.arvot() == null) {
      return Set.of();
    }
    return jakauma.arvot().stream()
        .map(ArvoDto::arvo)
        .limit(MAX_OSAAMISET)
        .map(URI::create)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /** Returns the text in Finnish, falling back to any language. */
  private static String localized(LocalizedString text) {
    if (text == null) {
      return "";
    }
    return Stream.concat(Stream.of(KIELI), Stream.of(Kieli.values()))
        .map(text::get)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse("");
  }

  private static String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength);
  }
}
