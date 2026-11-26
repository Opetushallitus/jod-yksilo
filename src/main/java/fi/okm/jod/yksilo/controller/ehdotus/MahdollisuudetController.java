/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.dto.SuunnitelmaEhdotusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusDto;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.KuvausData;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.Request;
import fi.okm.jod.yksilo.service.ehdotus.Suggestion;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.ToimenkuvaService;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  private final MahdollisuusEhdotusService mahdollisuusEhdotusService;
  private final MahdollisuudetService mahdollisuudetService;
  private final ToimenkuvaService toimenkuvaService;
  private final KoulutusService koulutusService;
  private final ToimintoService toimintoService;

  MahdollisuudetController(
      MahdollisuusEhdotusService mahdollisuusEhdotusService,
      MahdollisuudetService mahdollisuudetService,
      ToimenkuvaService toimenkuvaService,
      KoulutusService koulutusService,
      ToimintoService toimintoService) {
    this.mahdollisuusEhdotusService = mahdollisuusEhdotusService;
    this.mahdollisuudetService = mahdollisuudetService;
    this.toimenkuvaService = toimenkuvaService;
    this.koulutusService = koulutusService;
    this.toimintoService = toimintoService;
  }

  @PostMapping
  public List<EhdotusDto> createEhdotus(
      @RequestHeader(value = HttpHeaders.CONTENT_LANGUAGE, defaultValue = "fi") Kieli lang,
      @RequestParam(defaultValue = "asc") Sort.Direction sort,
      @RequestBody @Valid LuoEhdotusDto ehdotus,
      @AuthenticationPrincipal JodUser user) {
    log.info("Creating the suggestions");

    if (!mahdollisuusEhdotusService.supports(lang)) {
      throw new IllegalArgumentException("Unsupported language: " + lang);
    }

    final var mahdollisuusIds =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(sort, lang);

    final var osaamiset = mahdollisuusEhdotusService.knownOsaamiset(ehdotus.osaamiset());

    final var osaamisetVapaateksti = ehdotus.osaamisetVapaateksti;

    final var kiinnostukset = mahdollisuusEhdotusService.knownOsaamiset(ehdotus.kiinnostukset());

    final var kiinnostuksetVapaateksti = ehdotus.kiinnostuksetVapaateksti;

    final var kuvaukset =
        ehdotus.kuvaukset() == null ? Set.<LuoEhdotusKuvausDto>of() : ehdotus.kuvaukset();

    if (osaamiset.isEmpty()
        && (osaamisetVapaateksti == null || osaamisetVapaateksti.isBlank())
        && kiinnostukset.isEmpty()
        && (kiinnostuksetVapaateksti == null || kiinnostuksetVapaateksti.isBlank())
        && kuvaukset.isEmpty()) {
      return populateEmptyEhdotusDtos(mahdollisuusIds);
    }

    final Set<KuvausData> kuvauksetData =
        (user != null && ehdotus.kuvaukset != null)
            ? fetchKuvauksetData(user, lang, ehdotus.kuvaukset)
            : Set.of();

    // fetch kohtaanto results from inference endpoint and populate ehdotus DTOs
    return populateEhdotusDtos(
        mahdollisuusIds,
        performInferenceRequest(lang, ehdotus, osaamiset, kiinnostukset, kuvauksetData));
  }

  private Map<UUID, Suggestion> performInferenceRequest(
      Kieli lang,
      LuoEhdotusDto ehdotus,
      Set<URI> osaamiset,
      Set<URI> kiinnostukset,
      Set<KuvausData> kuvaukset) {
    // TODO: add language support when supported by kohtaanto inference endpoint
    return mahdollisuusEhdotusService.infer(
        lang,
        new Request.Data(
            osaamiset,
            ehdotus.osaamisetVapaateksti,
            ehdotus.osaamisPainotus,
            kiinnostukset,
            ehdotus.kiinnostuksetVapaateksti,
            ehdotus.kiinnostusPainotus,
            ehdotus.escoListPainotus,
            ehdotus.openTextPainotus,
            kuvaukset));
  }

  private List<EhdotusDto> populateEmptyEhdotusDtos(Map<UUID, MahdollisuusDto> ids) {
    final var counter = new AtomicInteger(0);
    return ids.entrySet().stream()
        .map(
            entry ->
                new EhdotusDto(
                    entry.getKey(),
                    EhdotusMetadata.empty(
                        entry.getValue().tyyppi(),
                        entry.getValue().ammattiryhma(),
                        entry.getValue().aineisto(),
                        entry.getValue().koulutusTyyppi(),
                        entry.getValue().maakunnat(),
                        entry.getValue().toimialat(),
                        entry.getValue().koulutusalat(),
                        entry.getValue().kesto(),
                        entry.getValue().kestoMinimi(),
                        entry.getValue().kestoMaksimi(),
                        counter.getAndIncrement())))
        .toList();
  }

  private List<EhdotusDto> populateEhdotusDtos(
      SequencedMap<UUID, MahdollisuusDto> ids, Map<UUID, Suggestion> suggestions) {
    // initialize lexicalIndex counters
    final var counter = new AtomicInteger(0);
    return ids.entrySet().stream()
        .map(
            entry -> {

              // Retrieve the result or create a new Suggestion if not found
              Suggestion suggestion =
                  Optional.ofNullable(suggestions.get(entry.getKey()))
                      .orElseGet(supplyEmptySuggestion(entry.getKey(), entry.getValue().tyyppi()));

              // Create and return EhdotusDto
              return new EhdotusDto(
                  suggestion.id(),
                  new EhdotusMetadata(
                      MahdollisuusTyyppi.valueOf(suggestion.type()),
                      entry.getValue().ammattiryhma(),
                      entry.getValue().aineisto(),
                      entry.getValue().koulutusTyyppi(),
                      entry.getValue().maakunnat(),
                      entry.getValue().toimialat(),
                      entry.getValue().koulutusalat(),
                      entry.getValue().kesto(),
                      entry.getValue().kestoMinimi(),
                      entry.getValue().kestoMaksimi(),
                      suggestion.score() >= 0 ? suggestion.score() : null,
                      null,
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

  private Set<KuvausData> fetchKuvauksetData(
      JodUser user, Kieli lang, Set<LuoEhdotusKuvausDto> kuvaukset) {
    var idsByType =
        kuvaukset.stream()
            .collect(
                Collectors.groupingBy(
                    LuoEhdotusKuvausDto::tyyppi,
                    Collectors.mapping(LuoEhdotusKuvausDto::id, Collectors.toSet())));

    var toimenkuvaMap =
        fetchByType(
            idsByType,
            OsaamisenLahdeTyyppi.TOIMENKUVA,
            ids -> toimenkuvaService.findAllByIds(user, ids));
    var koulutusMap =
        fetchByType(
            idsByType,
            OsaamisenLahdeTyyppi.KOULUTUS,
            ids -> koulutusService.findAllByIds(user, ids));
    var toimintoMap =
        fetchByType(
            idsByType,
            OsaamisenLahdeTyyppi.TOIMINTO,
            ids -> toimintoService.findAllByIds(user, ids));

    return kuvaukset.stream()
        .map(k -> toKuvausData(k, lang, toimenkuvaMap, koulutusMap, toimintoMap))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static <T> Map<UUID, T> fetchByType(
      Map<OsaamisenLahdeTyyppi, Set<UUID>> idsByType,
      OsaamisenLahdeTyyppi tyyppi,
      java.util.function.Function<Set<UUID>, Map<UUID, T>> fetcher) {
    var ids = idsByType.get(tyyppi);
    return (ids == null || ids.isEmpty()) ? Map.of() : fetcher.apply(ids);
  }

  private static KuvausData toKuvausData(
      LuoEhdotusKuvausDto kuvaus,
      Kieli lang,
      Map<UUID, ToimenkuvaDto> toimenkuvaMap,
      Map<UUID, KoulutusDto> koulutusMap,
      Map<UUID, ToimintoDto> toimintoMap) {
    return switch (kuvaus.tyyppi()) {
      case TOIMENKUVA -> {
        var d = toimenkuvaMap.get(kuvaus.id());
        yield d == null
            ? null
            : new KuvausData(
                d.nimi().get(lang),
                d.kuvaus() != null ? d.kuvaus().get(lang) : null,
                d.alkuPvm(),
                d.loppuPvm());
      }
      case KOULUTUS -> {
        var d = koulutusMap.get(kuvaus.id());
        yield d == null
            ? null
            : new KuvausData(
                d.nimi().get(lang),
                d.kuvaus() != null ? d.kuvaus().get(lang) : null,
                d.alkuPvm(),
                d.loppuPvm());
      }
      case TOIMINTO -> {
        var d = toimintoMap.get(kuvaus.id());
        yield d == null
            ? null
            : new KuvausData(
                d.nimi().get(lang),
                d.kuvaus() != null ? d.kuvaus().get(lang) : null,
                d.alkuPvm(),
                d.loppuPvm());
      }
      default -> {
        log.warn("Unexpected kuvaus tyyppi: {}", kuvaus.tyyppi());
        yield null;
      }
    };
  }

  private static Supplier<Suggestion> supplyEmptySuggestion(UUID key, MahdollisuusTyyppi tyyppi) {
    return () -> {
      log.warn("kohtaanto id {} not found from tyomahdollisuus or koulutus IDs", key);
      return new Suggestion(key, -1, tyyppi.name());
    };
  }

  /**
   * These trend values will become more precise as the definition becomes more precise.
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

  /**
   * This record describes metadata related to the proposal, based on which the proposals can be
   * sorted or filtered.
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
      String ammattiryhma,
      TyomahdollisuusAineisto aineisto,
      KoulutusmahdollisuusTyyppi koulutusmahdollisuusTyyppi,
      List<String> maakunnat,
      List<String> toimialat,
      List<String> koulutusalat,
      Double kesto,
      Double kestoMinimi,
      Double kestoMaksimi,
      Double pisteet,
      Trendi trendi,
      Long osaamisia,

      /* Value from 0 to */
      Integer tyollisyysNakyma,
      @NotNull Integer aakkosIndeksi) {

    public static EhdotusMetadata empty(
        MahdollisuusTyyppi tyyppi,
        String ammattiryhma,
        TyomahdollisuusAineisto aineisto,
        KoulutusmahdollisuusTyyppi koulutusmahdollisuusTyyppi,
        List<String> maakunnat,
        List<String> toimialat,
        List<String> koulutusalat,
        Double kesto,
        Double kestoMinimi,
        Double kestoMaksimi,
        int order) {
      return new EhdotusMetadata(
          tyyppi,
          ammattiryhma,
          aineisto,
          koulutusmahdollisuusTyyppi,
          maakunnat,
          toimialat,
          koulutusalat,
          kesto,
          kestoMinimi,
          kestoMaksimi,
          null,
          null,
          null,
          null,
          order);
    }
  }

  /**
   * This record models a proposal by including a reference to an opportunity that contains more
   * detailed information about the opportunity. For example: {@link TyomahdollisuusDto}.
   *
   * @param mahdollisuusId Opportunity ID
   * @param ehdotusMetadata Proposal metadata
   */
  public record EhdotusDto(UUID mahdollisuusId, EhdotusMetadata ehdotusMetadata) {}

  /**
   * This record models the create ehdotus request.
   *
   * @param osaamiset This is the list of skills ESCO URIs which are considered as skills of the
   *     customer related to know how.
   * @param osaamisetVapaateksti This is the free-form text related to osaamiset. This can be used
   *     to provide additional information about the skills of the customer that
   * @param osaamisPainotus This is the emphasis of osaamiset (skills related to know how).
   * @param kiinnostukset This is the list of skills ESCO URIs which are considered as skills of the
   *     customer related to kiinnostus.
   * @param kiinnostuksetVapaateksti This is the free text related to kiinnostukset. This can be
   *     used to provide additional information about the skills of the customer that
   * @param kiinnostusPainotus is the emphasis of kiinnostus (skills related to personal fields of
   *     interests).
   * @param escoListPainotus is the emphasis of osaamiset and kiinnostukset which are in ESCO list.
   * @param openTextPainotus is the emphasis of osaamiset and kiinnostukset which are provided as
   *     open text or not in ESCO list.
   * @param kuvaukset This is a list of description from the profile.
   */
  public record LuoEhdotusDto(
      @Size(max = 1000) Set<@Valid URI> osaamiset,
      @Size(max = 10000) String osaamisetVapaateksti,
      double osaamisPainotus,
      @Size(max = 1000) Set<@Valid URI> kiinnostukset,
      @Size(max = 10000) String kiinnostuksetVapaateksti,
      double kiinnostusPainotus,
      double escoListPainotus,
      double openTextPainotus,
      @Size(max = 1000) Set<LuoEhdotusKuvausDto> kuvaukset) {}

  public record LuoEhdotusKuvausDto(@NotNull OsaamisenLahdeTyyppi tyyppi, @NotNull UUID id) {}

  @PostMapping("/polku")
  @Operation(
      summary =
          "Gets suggestions for education opportunities for polku ranked suggestions based on missing skills.")
  public List<EhdotusDto> getMahdollisuudetSuggestionsForPolkuVaihe(
      @RequestBody
          @Valid
          @Size(max = 1000)
          @Schema(description = "Missing osaamiset that user did not selected that they know.")
          Set<@Valid URI> missingOsaamiset) {
    final List<SuunnitelmaEhdotusDto> suggestions =
        mahdollisuudetService.getPolkuVaiheSuggestions(missingOsaamiset);

    return populateEhdotusDtos(suggestions);
  }

  private static List<EhdotusDto> populateEhdotusDtos(List<SuunnitelmaEhdotusDto> suggestions) {
    return suggestions.stream()
        .map(
            it ->
                new EhdotusDto(
                    it.mahdollisuusId(),
                    new EhdotusMetadata(
                        MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS,
                        null,
                        null,
                        it.tyyppi(),
                        null,
                        null,
                        null,
                        it.kestoMediaani(),
                        it.kestoMinimi(),
                        it.kestoMaksimi(),
                        it.pisteet(),
                        null,
                        it.osaamisia(),
                        null,
                        0)))
        .toList();
  }
}
