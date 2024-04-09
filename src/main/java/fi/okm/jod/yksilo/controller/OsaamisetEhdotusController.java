/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Mock controller */
@RestController
@RequestMapping(path = "/api/ehdotus/osaamiset")
@Slf4j
public class OsaamisetEhdotusController {

  private final RestClient restClient;
  private static final Pattern NON_WORD =
      Pattern.compile("[^\\w\\p{Punct}]+", Pattern.UNICODE_CHARACTER_CLASS);

  public OsaamisetEhdotusController(
      RestClient.Builder restClientBuilder,
      MappingJackson2HttpMessageConverter messageConverter,
      @Value("${recommendation.skills.baseUrl}") String baseUrl) {
    log.info("Creating OsaamisetEhdotusController, baseUrl: {}", baseUrl);

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
            .build();

    // NOTE:
    // Default RestClient includes response bodies in error messages and stack traces, which
    // can be a security risk. RestClient also does not bound the response size in any way.

  }

  @PostMapping
  public ResponseEntity<List<Osaaminen>> createOsaamisetEhdotus(@RequestBody @Valid Taidot taidot) {
    final String input = normalize(taidot.kuvaus());

    record Input(String text, int maxNumberOfSkills, int maxNumberOfOccupations) {}
    record Skill(URI uri, String label, URI skillType, double score) {}
    record Result(List<Skill> skills) {}

    try {
      var result = restClient.post().body(new Input(input, 13, 1)).retrieve().body(Result.class);

      if (result == null || result.skills() == null) {
        return ResponseEntity.ok(List.of());
      }

      return ResponseEntity.ok(
          result.skills().stream()
              .map(s -> new Osaaminen(s.uri(), s.label(), s.skillType(), s.score()))
              .toList());

    } catch (RestClientException e) {
      log.error("Request failed", e);
      // todo: better error handling (should really throw an appropriate exception)
      return ResponseEntity.status(502).body(List.of());
    }
  }

  private static String normalize(String str) {
    return NON_WORD.matcher(Normalizer.normalize(str, Form.NFKC)).replaceAll(" ").trim();
  }

  public record Osaaminen(URI id, String nimi, URI tyyppi, double osuvuus) {}

  public record Taidot(@NotNull @Size(min = 1, max = 10_000) String kuvaus) {}
}
