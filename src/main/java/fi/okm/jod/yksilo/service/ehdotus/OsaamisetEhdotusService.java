/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.service.AmmattiService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.ServiceException;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Service for creating suggestions for Osaaminen using an external recommendation service. */
@Service
@Slf4j
public class OsaamisetEhdotusService {

  public static final int MAX_NUMBER_OF_SKILLS = 37;
  public static final int MAX_NUMBER_OF_OCCUPATIONS = 13;
  private final RestClient restClient;
  private final OsaaminenService osaamiset;
  private final AmmattiService ammatit;

  public OsaamisetEhdotusService(
      OsaaminenService osaamiset,
      AmmattiService ammatit,
      RestClient.Builder restClientBuilder,
      MappingJackson2HttpMessageConverter messageConverter,
      @Value("${jod.recommendation.skills.baseUrl}") String baseUrl) {
    log.info("Creating OsaamisetEhdotusService, baseUrl: {}", baseUrl);

    this.osaamiset = osaamiset;
    this.ammatit = ammatit;

    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .withHttpClientCustomizer(
                builder -> {
                  builder.connectTimeout(Duration.ofSeconds(5));
                })
            .withCustomizer(
                c -> {
                  c.setReadTimeout(Duration.ofSeconds(20));
                })
            .build();

    this.restClient =
        restClientBuilder
            .requestFactory(requestFactory)
            .messageConverters(
                converters -> {
                  converters.clear();
                  converters.add(messageConverter);
                })
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.USER_AGENT, "fi.okm.jod.yksilo")
            .build();
  }

  public List<Ehdotus> createEhdotus(LocalizedString kuvaus) {

    record Input(String text, int maxNumberOfSkills, int maxNumberOfOccupations, Kieli language) {}
    record Skill(URI uri, String label, URI skillType, double score) {}
    record Occupation(URI uri, String label, double score) {}
    record Result(List<Skill> skills, List<Occupation> occupations) {}

    log.info("Creating a suggestion for osaamiset");
    try {
      var entry = kuvaus.asMap().entrySet().iterator().next();
      var result =
          restClient
              .post()
              .body(
                  new Input(
                      entry.getValue(),
                      MAX_NUMBER_OF_SKILLS,
                      MAX_NUMBER_OF_OCCUPATIONS,
                      entry.getKey()))
              .retrieve()
              .body(Result.class);

      if (result == null || (result.skills() == null && result.occupations() == null)) {
        return List.of();
      }

      var escoIdentifiers = osaamiset.getAll().keySet();
      var escoOccupationIdentifiers = ammatit.getAll().keySet();

      List<Ehdotus> skills =
          result.skills().stream()
              .filter(
                  s -> {
                    var exists = escoIdentifiers.contains(s.uri());
                    if (!exists && log.isDebugEnabled()) {
                      log.debug("Unknown ESCO skill: {}", s.uri());
                    }
                    return exists;
                  })
              .map(s -> new Ehdotus(s.uri, s.score()))
              .toList();

      List<Ehdotus> occupations =
          result.occupations().stream()
              .filter(
                  o -> {
                    var exists = escoOccupationIdentifiers.contains(o.uri());
                    if (!exists && log.isDebugEnabled()) {
                      log.debug("Unknown ESCO occupation: {}", o.uri());
                    }
                    return exists;
                  })
              .map(s -> new Ehdotus(s.uri, s.score()))
              .sorted((a, b) -> Double.compare(b.osuvuus(), a.osuvuus()))
              .toList();

      return Stream.of(skills, occupations).flatMap(List::stream).collect(Collectors.toList());

    } catch (RestClientException e) {
      log.error("Request failed", e);
      throw new ServiceException("Failed to create ehdotus", e);
    }
  }

  public record Ehdotus(@NotNull URI uri, @NotNull double osuvuus) {}
}
