/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.AmmattiDto;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.service.AmmattiService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.inference.InferenceEndpoint;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/** Ranks opportunities (mahdollisuudet) using the kohtaanto inference endpoint. */
@Service
@Slf4j
public class MahdollisuusEhdotusService {

  private final InferenceService<Request, Response> inferenceService;
  private final Map<Kieli, InferenceEndpoint> endpoints;
  private final OsaaminenService osaaminenService;
  private final AmmattiService ammattiService;

  @ConfigurationProperties("jod.recommendation.mahdollisuus")
  public record EndpointProperties(Map<Kieli, String> endpoints) {}

  public MahdollisuusEhdotusService(
      InferenceService<Request, Response> inferenceService,
      EndpointProperties properties,
      OsaaminenService osaaminenService,
      AmmattiService ammattiService) {
    this.inferenceService = inferenceService;
    this.endpoints =
        properties.endpoints().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> InferenceEndpoint.of(e.getValue())));
    this.osaaminenService = osaaminenService;
    this.ammattiService = ammattiService;

    log.info("Creating MahdollisuusEhdotusService, endpoints: {}", endpoints);
  }

  /** Returns whether the inference endpoint is configured for the language. */
  public boolean supports(Kieli lang) {
    return endpoints.containsKey(lang);
  }

  /**
   * Returns the osaamiset and ammatit known by the backend. Only those are sent to the inference
   * endpoint.
   */
  public Set<URI> knownOsaamiset(Set<URI> uris) {
    if (uris == null || uris.isEmpty()) {
      return Set.of();
    }
    return Stream.concat(
            osaaminenService.findBy(uris).stream().map(OsaaminenDto::uri),
            ammattiService.findBy(uris).stream().map(AmmattiDto::uri))
        .collect(Collectors.toSet());
  }

  /**
   * Ranks the opportunities.
   *
   * @return the suggestions by the opportunity ID, in the order returned by the inference endpoint.
   *     The scores are relative to the request, so they are not comparable between requests.
   * @throws IllegalArgumentException if the language is not supported
   */
  public Map<UUID, Suggestion> infer(Kieli lang, Request.Data data) {
    var endpoint = endpoints.get(lang);
    if (endpoint == null) {
      throw new IllegalArgumentException("Unsupported language: " + lang);
    }
    return inferenceService
        .infer(endpoint, new Request(data), new ParameterizedTypeReference<>() {})
        .stream()
        .collect(
            Collectors.toMap(
                Suggestion::id, s -> s, (existing, duplicate) -> existing, LinkedHashMap::new));
  }

  public record Request(Data data) {
    public record Data(
        Set<URI> osaamiset,
        String osaamisetText,
        double osaamisPainotus,
        Set<URI> kiinnostukset,
        String kiinnostuksetText,
        double kiinnostusPainotus,
        double escoListPainotus,
        double openTextPainotus,
        Set<KuvausData> kuvaukset) {}
  }

  public record KuvausData(String otsikko, String kuvaus, LocalDate alkuPvm, LocalDate loppuPvm) {}

  @SuppressWarnings("serial")
  public static class Response extends ArrayList<Suggestion> {}
}
