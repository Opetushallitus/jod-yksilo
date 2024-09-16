/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.controller.ehdotus.TyomahdollisuudetController.Request.Data;
import fi.okm.jod.yksilo.controller.ehdotus.TyomahdollisuudetController.Response.Suggestion;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
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
@RequestMapping(path = "/api/ehdotus/tyomahdollisuudet")
@Slf4j
@Tag(name = "ehdotus")
class TyomahdollisuudetController {

  private final InferenceService<Request, Response> inferenceService;
  private final String endpoint;
  private final TyomahdollisuusService tyomahdollisuusService;
  private final OsaaminenService osaaminenService;

  TyomahdollisuudetController(
      InferenceService<Request, Response> inferenceService,
      @Value("${jod.recommendation.tyomahdollisuus.endpoint}") String endpoint,
      TyomahdollisuusService tyomahdollisuusService,
      OsaaminenService osaaminenService) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
    this.tyomahdollisuusService = tyomahdollisuusService;
    this.osaaminenService = osaaminenService;

    log.info("Creating TyomahdollisuudetController, endpoint: {}", endpoint);
  }

  @PostMapping
  @Timed
  public List<EhdotusDto> createEhdotus(@RequestBody @Valid LuoEhdotusDto ehdotus) {

    var tyomahdollisuusMetaData = tyomahdollisuusService.fetchAllTyomahdollisuusMetadata();

    log.info("Creating a suggestion for tyomahdollisuudet");

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
      var emptyMetadata =
          new EhdotusMetadata(OptionalDouble.empty(), Optional.empty(), OptionalInt.empty());
      return tyomahdollisuusMetaData.keySet().stream()
          .map(key -> new EhdotusDto(key, emptyMetadata))
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
            .collect(Collectors.toMap(Suggestion::id, Suggestion::score, Double::max));

    return tyomahdollisuusMetaData.keySet().stream()
        .map(
            key ->
                new EhdotusDto(
                    key,
                    new EhdotusMetadata(
                        OptionalDouble.of(
                            result.get(tyomahdollisuusMetaData.get(key).externalId())),
                        Optional.empty(),
                        OptionalInt.empty())))
        .sorted(Comparator.comparing(e -> e.ehdotusMetadata.pisteet.orElse(0d)))
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
      OptionalDouble pisteet,
      Optional<Trendi> trendi,

      /** Value from 0 to */
      OptionalInt tyollisyysNakyma) {}

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
    record Suggestion(UUID id, String name, double score) {}
  }
}
