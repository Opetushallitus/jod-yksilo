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
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.AmmattiDto;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.service.AmmattiService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ehdotus/mahdollisuudet")
@Slf4j
@Tag(name = "ehdotus")
class MahdollisuudetController {

  private final InferenceService<Request, Response> inferenceService;
  private final String endpoint;
  private final MahdollisuudetService mahdollisuudetService;
  private final OsaaminenService osaaminenService;
  private final AmmattiService ammattiService;

  MahdollisuudetController(
      InferenceService<Request, Response> inferenceService,
      @Value("${jod.recommendation.mahdollisuus.endpoint}") String endpoint,
      MahdollisuudetService mahdollisuudetService,
      OsaaminenService osaaminenService,
      AmmattiService ammattiService) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
    this.mahdollisuudetService = mahdollisuudetService;
    this.osaaminenService = osaaminenService;
    this.ammattiService = ammattiService;

    log.info("Creating TyomahdollisuudetController, endpoint: {}", endpoint);
  }

  @PostMapping
  @Timed
  public List<EhdotusDto> createEhdotus(
      @RequestHeader(value = HttpHeaders.CONTENT_LANGUAGE, defaultValue = "fi") Kieli lang,
      @RequestParam(defaultValue = "asc") Sort.Direction sort,
      @RequestBody @Valid LuoEhdotusDto ehdotus) {
    log.info("Creating the suggestions");
    final var mahdollisuusIds =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(sort, lang);

    final var osaamiset =
        ehdotus.osaamiset() == null
            ? Set.<URI>of()
            : Stream.concat(
                    osaaminenService.findBy(ehdotus.osaamiset).stream().map(OsaaminenDto::uri),
                    ammattiService.findBy(ehdotus.osaamiset).stream().map(AmmattiDto::uri))
                .collect(Collectors.toSet());

    final var kiinnostukset =
        ehdotus.kiinnostukset() == null
            ? Set.<URI>of()
            : Stream.concat(
                    osaaminenService.findBy(ehdotus.kiinnostukset).stream().map(OsaaminenDto::uri),
                    ammattiService.findBy(ehdotus.kiinnostukset).stream().map(AmmattiDto::uri))
                .collect(Collectors.toSet());

    if (osaamiset.isEmpty() && kiinnostukset.isEmpty()) {
      // if osaamiset and kiinnostukset is empty return list of työmahdollisuuksia with empty
      // metadata
      return populateEmptyEhdotusDtos(mahdollisuusIds);
    }

    // fetch kohtaanto results from inference endpoint and populate ehdotus DTOs
    return populateEhdotusDtos(
        mahdollisuusIds, performInferenceRequest(ehdotus, osaamiset, kiinnostukset));
  }

  private Map<UUID, Suggestion> performInferenceRequest(
      LuoEhdotusDto ehdotus, Set<URI> osaamiset, Set<URI> kiinnostukset) {
    // TODO: add language support when supported by kohtaanto inference endpoint
    var request =
        new Request(
            new Data(
                ehdotus.osaamisPainotus, osaamiset, ehdotus.kiinnostusPainotus, kiinnostukset));

    return inferenceService.infer(endpoint, request, Response.class).stream()
        .collect(Collectors.toMap(Suggestion::id, r -> r, (exising, newValue) -> exising));
  }

  private List<EhdotusDto> populateEmptyEhdotusDtos(LinkedHashMap<UUID, MahdollisuusTyyppi> ids) {
    final var counter = new AtomicInteger(0);
    return ids.entrySet().stream()
        .map(
            entry ->
                new EhdotusDto(
                    entry.getKey(),
                    EhdotusMetadata.empty(entry.getValue(), counter.getAndIncrement())))
        .toList();
  }

  private List<EhdotusDto> populateEhdotusDtos(
      LinkedHashMap<UUID, MahdollisuusTyyppi> ids, Map<UUID, Suggestion> suggestions) {
    // initialize lexicalIndex counters
    final var counter = new AtomicInteger(0);
    return ids.entrySet().stream()
        .map(
            entry -> {

              // Retrieve the result or create a new Suggestion if not found
              Suggestion suggestion =
                  Optional.ofNullable(suggestions.get(entry.getKey()))
                      .orElseGet(supplyEmptySuggestion(entry.getKey(), entry.getValue()));

              // Create and return EhdotusDto
              return new EhdotusDto(
                  suggestion.id,
                  new EhdotusMetadata(
                      MahdollisuusTyyppi.valueOf(suggestion.type),
                      suggestion.score >= 0 ? suggestion.score : null,
                      null,
                      null,
                      counter.getAndIncrement()));
            })
        .sorted(
            Comparator.comparingDouble(
                    (EhdotusDto e) ->
                        e.ehdotusMetadata.pisteet != null ? e.ehdotusMetadata.pisteet : 0)
                .reversed())
        .toList();
  }

  private static Supplier<Suggestion> supplyEmptySuggestion(UUID key, MahdollisuusTyyppi tyyppi) {
    return () -> {
      log.warn("kohtaanto id {} not found from tyomahdollisuus or koulutus IDs", key);
      return new Suggestion(key, -1, tyyppi.name());
    };
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

  public record Request(Data data) {
    record Data(
        double osaamisPainotus,
        Set<URI> osaamiset,
        double kiinnostusPainotus,
        Set<URI> kiinnostukset) {}
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
      @Nullable Integer tyollisyysNakyma,
      @NotNull Integer aakkosIndeksi) {

    public static EhdotusMetadata empty(MahdollisuusTyyppi tyyppi, int order) {
      return new EhdotusMetadata(tyyppi, null, null, null, order);
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
    record Suggestion(UUID id, double score, String type) {}
  }
}
