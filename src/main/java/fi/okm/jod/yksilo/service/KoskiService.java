/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import com.jayway.jsonpath.JsonPath;
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

  public KoskiService(
      RestClient.Builder restClientBuilder, MappingJackson2HttpMessageConverter messageConverter) {
    log.info("Creating KoskiService");

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

  private <T> T readJsonProperty(Object json, String jsonPathExpression) {
    try {
      return JsonPath.read(json, jsonPathExpression);
    } catch (Exception e) {
      log.debug("JSON expression error", e);
      return null;
    }
  }

  public List<KoulutusDto> getKoskiData(URI linkki) {
    if (!allowedHosts.contains(linkki.getHost())) {
      final var koskiResponse = restClient.get().uri(linkki).retrieve().body(Object.class);

      List<Object> opinnot = readJsonProperty(koskiResponse, "$.opiskeluoikeudet");
      if (opinnot == null) {
        return Collections.emptyList();
      }

      return opinnot.stream()
          .map(
              o -> {
                var toimija =
                    readJsonProperty(o, "$.oppilaitos") != null
                        ? readJsonProperty(o, "$.oppilaitos")
                        : readJsonProperty(o, "$.koulutustoimija");
                var nimet = readJsonProperty(toimija, "$.nimi");
                var localizedNimi = getLocalizedString(nimet);
                var suoritukset =
                    readJsonProperty(o, "$.suoritukset[0].koulutusmoduuli.tunniste.nimi");
                var localizedKuvaus = getLocalizedString(suoritukset);

                String alkuExpression = "$.alkamispäivä";

                if (linkki.getPath().contains("/suoritetut-tutkinnot/")) {
                  // read vahvistettu
                  alkuExpression = "$.suoritukset[0].vahvistus.päivä";
                }
                var start = getLocalDate(o, alkuExpression);
                var alkoi = start == null ? LocalDate.ofEpochDay(0) : start;
                var loppui = getLocalDate(o, "$.päättymispäivä");

                return new KoulutusDto(null, localizedNimi, localizedKuvaus, alkoi, loppui, null);
              })
          .toList();
    }
    throw new IllegalArgumentException("invalid opintopolku URL");
  }

  private LocalizedString getLocalizedString(Object nimet) {

    var nonEmptyLocalizedValues =
        Stream.of(Kieli.values())
            .map(kieli -> Map.entry(kieli, getString(nimet, "$." + kieli.name().toLowerCase())))
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (existing, replacement) -> existing,
                    () -> new EnumMap<>(Kieli.class)));

    return new LocalizedString(nonEmptyLocalizedValues);
  }

  private String getString(Object nimet, String expression) {
    return readJsonProperty(nimet, expression) != null ? readJsonProperty(nimet, expression) : "";
  }

  private LocalDate getLocalDate(Object nimet, String expression) {
    String dateString = readJsonProperty(nimet, expression);
    return dateString == null ? null : LocalDate.parse(dateString);
  }
}
