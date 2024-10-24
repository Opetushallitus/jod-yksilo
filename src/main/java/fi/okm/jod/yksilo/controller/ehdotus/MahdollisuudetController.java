/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.controller.ehdotus.MahdollisuudetController.Request.Data;
import fi.okm.jod.yksilo.controller.ehdotus.MahdollisuudetController.Response.Suggestion;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ehdotus/mahdollisuudet")
@Slf4j
@Tag(name = "ehdotus")
class MahdollisuudetController {

  private final InferenceService<Request, Response> inferenceService;
  private final String endpoint;
  private final TyomahdollisuusService tyomahdollisuusService;
  private final KoulutusmahdollisuusService koulutusmahdollisuusService;
  private final OsaaminenService osaaminenService;

  MahdollisuudetController(
      InferenceService<Request, Response> inferenceService,
      @Value("${jod.recommendation.mahdollisuus.endpoint}") String endpoint,
      TyomahdollisuusService tyomahdollisuusService,
      KoulutusmahdollisuusService koulutusmahdollisuusService,
      OsaaminenService osaaminenService) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
    this.tyomahdollisuusService = tyomahdollisuusService;
    this.koulutusmahdollisuusService = koulutusmahdollisuusService;
    this.osaaminenService = osaaminenService;

    log.info("Creating TyomahdollisuudetController, endpoint: {}", endpoint);
  }

  private static MahdollisuusTyyppi getTyyppi(boolean tyo, boolean koulutus) {
    if (tyo) return MahdollisuusTyyppi.TYOMAHDOLLISUUS;
    if (koulutus) return MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
    throw new IllegalArgumentException(
        "Unknown mahdollisuus type. Either tyo or koulutus must be true.");
  }

  private static MahdollisuusTyyppi getTyyppi(UUID key, Set<UUID> tyomahdollisuusIds) {
    return tyomahdollisuusIds.contains(key)
        ? MahdollisuusTyyppi.TYOMAHDOLLISUUS
        : MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
  }

  @PostMapping
  @Timed
  public List<EhdotusDto> createEhdotus(@RequestBody @Valid LuoEhdotusDto ehdotus) {

    var tyomahdollisuusIds = tyomahdollisuusService.fetchAllIds();

    var ids = new HashSet<UUID>();
    ids.addAll(tyomahdollisuusIds);
    ids.addAll(koulutusmahdollisuusService.fetchAllIds());

    log.info("Creating the suggestions");

    var osaamiset =
        ehdotus.osaamiset() == null
            ? List.<OsaaminenDto>of()
            : osaaminenService.findBy(ehdotus.osaamiset);

    var kiinnostukset =
        ehdotus.kiinnostukset() == null
            ? List.<OsaaminenDto>of()
            : osaaminenService.findBy(ehdotus.kiinnostukset);

    if (osaamiset.isEmpty() && kiinnostukset.isEmpty()) {
      // if osaamiset and kiinnostukset is empty return list of työmahdollisuuksia with empty
      // metadata
      return ids.stream()
          .map(
              key -> new EhdotusDto(key, EhdotusMetadata.empty(getTyyppi(key, tyomahdollisuusIds))))
          .toList();
    }

    var request =
        new Request(
            new Data(
                ehdotus.osaamisPainotus,
                osaamiset.stream().map(o -> o.nimi().get(Kieli.FI)).toList(),
                ehdotus.kiinnostusPainotus,
                kiinnostukset.stream().map(o -> o.nimi().get(Kieli.FI)).toList()));

    var result =
        inferenceService.infer(endpoint, request, Response.class).stream()
            .collect(Collectors.toMap(Suggestion::id, r -> r, (exising, newValue) -> exising));

    return ids.stream()
        .map(
            key ->
                Optional.ofNullable(result.get(key))
                    .orElseGet(
                        () -> {
                          log.warn(
                              "kohtaanto id {} not found from tyomahdollisuus or koulutus IDs",
                              key.toString());
                          var tyyppi = getTyyppi(key, tyomahdollisuusIds);
                          return new Suggestion(
                              key,
                              -1,
                              tyyppi == MahdollisuusTyyppi.TYOMAHDOLLISUUS,
                              tyyppi == MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS);
                        }))
        .map(
            r ->
                new EhdotusDto(
                    r.id,
                    new EhdotusMetadata(
                        getTyyppi(r.tyo, r.koulutus), r.score >= 0 ? r.score : null, null, null)))
        .sorted(
            Comparator.comparingDouble(
                    (EhdotusDto e) ->
                        e.ehdotusMetadata.pisteet != null ? e.ehdotusMetadata.pisteet : 0)
                .reversed())
        .toList();
  }

  /**
   * These trend values will become more precise as the definition becomes more precise
   *
   * <h2>constant:</h2>
   *
   * <ul>
   *   <li>{@link #NOUSEVA} - The trend of this view is rising.
   *   <li>{@link #LASKEVA} - the trend in this view is down.
   * </ul>
   */
  public enum Trendi {
    NOUSEVA,
    LASKEVA
  }

  public enum MahdollisuusTyyppi {
    TYOMAHDOLLISUUS,
    KOULUTUSMAHDOLLISUUS
  }

  public record Request(Data data) {
    record Data(
        double osaamisPainotus,
        List<String> osaamiset,
        double kiinnostusPainotus,
        List<String> kiinnostukset) {}
  }

  /**
   * This record describes metadata related to the proposal, based on which the proposals can be
   * sorted or filtered
   *
   * @param pisteet The points indicate the suitability of this proposal with a value of 1.0 ... 0,
   *     where 1 is 100% suitability.
   * @param trendi The trend tells about the general development trend of the information attached
   *     to the proposal (optional).
   * @param tyollisyysNakyma The employment view shows how, according to the statistics, the
   *     opportunity associated with the proposal has been employed.
   */
  public record EhdotusMetadata(
      @NotNull MahdollisuusTyyppi tyyppi,
      @Nullable Double pisteet,
      @Nullable Trendi trendi,

      /** Value from 0 to */
      @Nullable Integer tyollisyysNakyma) {

    public static EhdotusMetadata empty(MahdollisuusTyyppi tyyppi) {
      return new EhdotusMetadata(tyyppi, null, null, null);
    }
  }

  /**
   * This record models a proposal by including a reference to an opportunity that contains more
   * detailed information about the opportunity. For example: {@link TyomahdollisuusDto}
   *
   * @param mahdollisuusId Opportunity ID
   * @param ehdotusMetadata Proposal metadata
   */
  public record EhdotusDto(UUID mahdollisuusId, EhdotusMetadata ehdotusMetadata) {}

  /**
   * This record models the create ehdotus request where
   *
   * @param osaamisPainotus This is the emphasis of osaamiset (skills related to know how).
   * @param osaamiset This is the list of skills ESCO URIs which are considered as skills of the
   *     customer related to know how.
   * @param kiinnostusPainotus is the emphasis of kiinnostus (skills related to personal fields of
   *     interests).
   * @param kiinnostukset This is the list of skills ESCO URIs which are considered as skills of the
   *     customer related to kiinnostus.
   */
  public record LuoEhdotusDto(
      double osaamisPainotus,
      @Size(max = 1000) Set<@Valid URI> osaamiset,
      double kiinnostusPainotus,
      @Size(max = 1000) Set<@Valid URI> kiinnostukset) {}

  @SuppressWarnings("serial")
  static class Response extends ArrayList<Suggestion> {
    record Suggestion(UUID id, double score, boolean tyo, boolean koulutus) {}
  }
}
