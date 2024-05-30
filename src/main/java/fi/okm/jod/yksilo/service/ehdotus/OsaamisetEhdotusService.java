/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.service.ServiceException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Mock controller */
@Service
@Slf4j
@Transactional(readOnly = true)
public class OsaamisetEhdotusService {

  private final RestClient restClient;
  private final OsaaminenRepository osaamiset;

  public OsaamisetEhdotusService(
      OsaaminenRepository osaamiset,
      RestClient.Builder restClientBuilder,
      MappingJackson2HttpMessageConverter messageConverter,
      @Value("${jod.recommendation.skills.baseUrl}") String baseUrl) {
    log.info("Creating OsaamisetEhdotusController, baseUrl: {}", baseUrl);

    this.osaamiset = osaamiset;

    var requestFactory =
        ClientHttpRequestFactories.get(
            ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(5000))
                .withReadTimeout(Duration.ofMillis(10000)));

    this.restClient =
        restClientBuilder
            .requestFactory(requestFactory)
            .messageConverters(
                converters -> {
                  converters.clear();
                  converters.add(messageConverter);
                })
            .baseUrl(baseUrl)
            .defaultHeader(
                HttpHeaders.USER_AGENT, "fi.okm.jod (https://okm.fi/hanke?tunnus=OKM069:00/2021)")
            .build();

    // NOTE:
    // Default RestClient includes response bodies in error messages and stack traces, which
    // can be a security risk. RestClient also does not bound the response size in any way.
  }

  public List<Ehdotus> createEhdotus(String kuvaus) {

    record Input(String text, int maxNumberOfSkills, int maxNumberOfOccupations) {}
    record Skill(URI uri, String label, URI skillType, double score) {}
    record Result(List<Skill> skills) {}

    log.info("Creating a suggestion for osaamiset");
    try {
      var result = restClient.post().body(new Input(kuvaus, 13, 1)).retrieve().body(Result.class);

      if (result == null || result.skills() == null) {
        return List.of();
      }

      var map =
          osaamiset
              .findByUriIn(result.skills().stream().map(s -> s.uri().toString()).toList())
              .stream()
              .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus()))
              .collect(Collectors.toMap(OsaaminenDto::uri, it -> it));

      return result.skills().stream()
          .filter(s -> map.containsKey(s.uri()))
          .map(s -> new Ehdotus(s.uri(), s.label(), s.skillType(), s.score()))
          .toList();

    } catch (RestClientException e) {
      log.error("Request failed", e);
      throw new ServiceException("Failed to create ehdotus", e);
    }
  }

  public record Ehdotus(URI id, String nimi, URI tyyppi, double osuvuus) {}
}
