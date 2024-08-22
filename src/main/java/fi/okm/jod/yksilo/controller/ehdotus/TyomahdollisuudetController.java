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
import java.util.List;
import java.util.Set;
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

    log.info("Creating a suggestion for tyomahdollisuudet");
    // temporarily using names instead of IDs

    var osaamiset =
        ehdotus.osaamiset() == null
            ? List.<OsaaminenDto>of()
            : osaaminenService.findBy(ehdotus.osaamiset);

    var kiinostukset =
        ehdotus.kiinnostukset() == null
            ? List.<OsaaminenDto>of()
            : osaaminenService.findBy(ehdotus.kiinnostukset);

    if (osaamiset.isEmpty() && kiinostukset.isEmpty()) {
      return List.of();
    }

    var request =
        new Request(
            new Data(
                ehdotus.osaamisPainotus,
                osaamiset.stream().map(o -> o.nimi().get(Kieli.FI)).toList(),
                ehdotus.kiinostusPainotus,
                kiinostukset.stream().map(o -> o.nimi().get(Kieli.FI)).toList()));

    var result =
        inferenceService.infer(endpoint, request, Response.class).stream()
            .collect(Collectors.toMap(Suggestion::name, Suggestion::score, Double::max));

    return tyomahdollisuusService.findByName(result.keySet()).stream()
        .map(
            tyomahdollisuusDto ->
                new EhdotusDto(
                    tyomahdollisuusDto, result.get(tyomahdollisuusDto.otsikko().get(Kieli.FI))))
        .sorted((a, b) -> Double.compare(b.osuvuus(), a.osuvuus()))
        .toList();
  }

  public record Request(Data data) {
    record Data(
        double osaamisPainotus,
        List<String> osaamiset,
        double kiinnostusPainotus,
        List<String> kiinnostukset) {}
  }

  public record EhdotusDto(TyomahdollisuusDto tyomahdollisuus, double osuvuus) {}

  public record LuoEhdotusDto(
      double osaamisPainotus,
      @Size(max = 1000) Set<@Valid URI> osaamiset,
      double kiinostusPainotus,
      @Size(max = 1000) Set<@Valid URI> kiinnostukset) {}

  @SuppressWarnings("serial")
  static class Response extends ArrayList<Suggestion> {
    record Suggestion(String name, double score) {}
  }
}
