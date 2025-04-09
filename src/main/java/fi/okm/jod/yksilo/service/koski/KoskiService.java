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
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.profiili.Mapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@ConditionalOnBean(KoskiOAuth2Config.class)
@Service
@Slf4j
public class KoskiService {

  private static final String KOULUTUSMODUULI_FIELD = "koulutusmoduuli";

  private final KoulutusRepository koulutusRepository;

  public KoskiService(KoulutusRepository koulutusRepository) {
    this.koulutusRepository = koulutusRepository;
    log.info("Creating KoskiService");
  }

  private static JsonNode readJsonProperty(JsonNode jsonNode, String... paths) {
    for (String path : paths) {
      if (jsonNode == null) {
        return null;
      }
      jsonNode = jsonNode.path(path);
    }
    return jsonNode.isMissingNode() ? null : jsonNode;
  }

  public List<KoulutusDto> getKoulutusData(JsonNode koskiResponse) {
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
              LocalizedString localizedKuvaus = null;
              if (suoritukset != null && suoritukset.isArray() && !suoritukset.isEmpty()) {
                var tunnisteNimiNode =
                    readJsonProperty(suoritukset.get(0), KOULUTUSMODUULI_FIELD, "tunniste", "nimi");
                var nimiNode = readJsonProperty(suoritukset.get(0), KOULUTUSMODUULI_FIELD, "nimi");
                localizedKuvaus =
                    getLocalizedKuvaus(
                        getLocalizedString(tunnisteNimiNode),
                        nimiNode == null ? null : getLocalizedString(nimiNode));
              }
              var alkoi = getLocalDate(o, "alkamispäivä");
              var loppui = getLocalDate(o, "päättymispäivä");
              var osasuoritukset =
                  suoritukset != null
                      ? getOsasuoritukset(readJsonProperty(suoritukset.get(0), "osasuoritukset"))
                      : null;

              return new KoulutusDto(
                  null,
                  localizedNimi,
                  localizedKuvaus,
                  alkoi,
                  loppui,
                  null,
                  true,
                  null,
                  osasuoritukset);
            })
        .toList();
  }

  private static Set<String> getOsasuoritukset(JsonNode osasuoritukset) {
    var osasuorituksetList = new HashSet<String>();
    if (osasuoritukset != null && osasuoritukset.isArray() && !osasuoritukset.isEmpty()) {
      for (var jsonNode : osasuoritukset) {
        var nimiNode = readJsonProperty(jsonNode, KOULUTUSMODUULI_FIELD, "nimi");
        if (nimiNode != null && !nimiNode.isEmpty()) {
          osasuorituksetList.add(getLocalizedString(nimiNode).get(Kieli.FI));
        }
      }
    }
    return osasuorituksetList;
  }

  static LocalizedString getLocalizedKuvaus(
      LocalizedString localizedKuvaus, LocalizedString localizedKuvausNimi) {
    if (localizedKuvaus == null) {
      return null;
    }
    if (localizedKuvausNimi == null || localizedKuvausNimi.asMap().isEmpty()) {
      return localizedKuvaus;
    }
    return new LocalizedString(
        localizedKuvaus.asMap().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                      String additionalDetailText = localizedKuvausNimi.get(entry.getKey());
                      return StringUtils.hasText(additionalDetailText)
                          ? entry.getValue() + ": " + additionalDetailText
                          : entry.getValue();
                    },
                    (v1, v2) -> v1,
                    () -> new EnumMap<>(Kieli.class))));
  }

  private static LocalizedString getLocalizedString(JsonNode nimet) {
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

  private static String getString(JsonNode nimet, String path) {
    JsonNode node = readJsonProperty(nimet, path);
    return node != null && node.isTextual() ? node.asText() : "";
  }

  private static LocalDate getLocalDate(JsonNode nimet, String path) {
    JsonNode node = readJsonProperty(nimet, path);
    return node != null && node.isTextual() ? LocalDate.parse(node.asText()) : null;
  }

  @Transactional(readOnly = true)
  public List<KoulutusDto> getOsaamisetIdentified(JodUser user, List<UUID> uuids) {
    var statusesToReturn = Set.of(OsaamisenTunnistusStatus.DONE, OsaamisenTunnistusStatus.FAIL);
    var koulutusList =
        koulutusRepository.findByKokonaisuusYksiloIdAndIdInAndOsaamisenTunnistusStatusIn(
            user.getId(), uuids, statusesToReturn);
    return koulutusList.stream().map(Mapper::mapKoulutus).toList();
  }
}
