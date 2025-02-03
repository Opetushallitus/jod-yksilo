/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class KoskiService {
  @Value("${jod.integraatio.koski.hosts:}.split(',')")
  private List<String> allowedHosts;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public KoskiService(
      RestClient.Builder restClientBuilder,
      MappingJackson2HttpMessageConverter messageConverter,
      ObjectMapper objectMapper) {
    log.info("Creating KoskiService");
    this.objectMapper = objectMapper;

    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(10));
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    this.restClient =
        restClientBuilder
            .messageConverters(
                converters -> {
                  converters.clear();
                  converters.add(messageConverter);
                })
            .requestFactory(requestFactory)
            .defaultHeader(
                HttpHeaders.USER_AGENT, "fi.okm.jod (https://okm.fi/hanke?tunnus=OKM069:00/2021)")
            .build();
  }

  private JsonNode readJsonProperty(JsonNode jsonNode, String... paths) {
    for (String path : paths) {
      if (jsonNode == null) {
        return null;
      }
      jsonNode = jsonNode.path(path);
    }
    return jsonNode.isMissingNode() ? null : jsonNode;
  }

  public List<KoulutusDto> getKoskiData(URI linkki) {
    if (!allowedHosts.contains(linkki.getHost())) {
      try {
        var koskiResponse = restClient.get().uri(linkki).retrieve().body(JsonNode.class);
        return getKoulutusData(koskiResponse, linkki);

      } catch (Exception e) {
        log.error("Failed to parse JSON response", e);
      }
    }
    throw new IllegalArgumentException("invalid opintopolku URL");
  }

  public List<KoulutusDto> getKoulutusData(JsonNode koskiResponse, URI linkki) {
    JsonNode opinnot = readJsonProperty(koskiResponse, "opiskeluoikeudet");
    if (opinnot == null || !opinnot.isArray()) {
      return Collections.emptyList();
    }

    return StreamSupport.stream(opinnot.spliterator(), false)
        .map(
            o -> {
              var toimija =
                  readJsonProperty(o, "oppilaitos") != null
                      ? readJsonProperty(o, "oppilaitos")
                      : readJsonProperty(o, "koulutustoimija");
              var nimet = readJsonProperty(toimija, "nimi");
              var localizedNimi = getLocalizedString(nimet);

              var suoritukset = readJsonProperty(o, "suoritukset");
              var koulutusmoduuli =
                  suoritukset != null && suoritukset.isArray() && !suoritukset.isEmpty()
                      ? readJsonProperty(suoritukset.get(0), "koulutusmoduuli", "tunniste", "nimi")
                      : null;
              var localizedKuvaus = getLocalizedString(koulutusmoduuli);

              var alkuExpression = "alkamispäivä";
              if (linkki != null && linkki.getPath().contains("/suoritetut-tutkinnot/")) {
                alkuExpression = "suoritukset[0].vahvistus.päivä";
              }
              var alkoi = getLocalDate(o, alkuExpression);
              var loppui = getLocalDate(o, "päättymispäivä");

              return new KoulutusDto(null, localizedNimi, localizedKuvaus, alkoi, loppui, null);
            })
        .toList();
  }

  private LocalizedString getLocalizedString(JsonNode nimet) {
    if (nimet == null || nimet.isMissingNode()) {
      return new LocalizedString(Collections.emptyMap());
    }

    Map<Kieli, String> localizedValues =
        Stream.of(Kieli.values())
            .map(kieli -> Map.entry(kieli, getString(nimet, kieli.name().toLowerCase())))
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> a,
                    () -> new EnumMap<>(Kieli.class)));

    return new LocalizedString(localizedValues);
  }

  private String getString(JsonNode nimet, String path) {
    JsonNode node = readJsonProperty(nimet, path);
    return node != null && node.isTextual() ? node.asText() : "";
  }

  private LocalDate getLocalDate(JsonNode nimet, String path) {
    JsonNode node = readJsonProperty(nimet, path);
    return node != null && node.isTextual() ? LocalDate.parse(node.asText()) : null;
  }
}
