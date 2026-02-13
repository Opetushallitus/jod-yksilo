/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.profiili.Mapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@ConditionalOnBean(KoskiOauth2Config.class)
@Service
@Slf4j
public class KoskiService {

  private final KoulutusRepository koulutusRepository;

  public KoskiService(KoulutusRepository koulutusRepository) {
    this.koulutusRepository = koulutusRepository;
    log.info("Creating KoskiService");
  }

  public List<KoulutusDto> mapKoulutusData(JsonNode koskiResponse) {
    if (koskiResponse == null) {
      return List.of();
    }

    var opinnot = koskiResponse.path("opiskeluoikeudet");

    return opinnot
        .valueStream()
        .flatMap(
            node -> {
              var toimija =
                  getLocalizedString(
                      node.has("oppilaitos")
                          ? node.path("oppilaitos").path("nimi")
                          : node.path("koulutustoimija").path("nimi"));

              if (toimija == null) {
                log.info(
                    "Koski opiskeluoikeus {} is missing toimija name, skipping",
                    node.path("oid").stringValue());
                return Stream.of();
              }

              var alkoi = getLocalDate(node.path("alkamispäivä"));
              var loppui = getLocalDate(node.path("päättymispäivä"));
              var suoritukset = node.path("suoritukset");

              LocalizedString kuvaus = null;
              Set<String> osasuoritukset = null;
              if (suoritukset.isArray() && !suoritukset.isEmpty()) {
                var moduuli = suoritukset.path(0).path("koulutusmoduuli");
                var tunniste = getLocalizedString(moduuli.path("tunniste").path("nimi"));
                var nimi = getLocalizedString(moduuli.path("nimi"));
                kuvaus = join(tunniste, nimi);
                osasuoritukset = getOsasuoritukset(suoritukset.path(0).path("osasuoritukset"));
              }

              return Stream.of(
                  new KoulutusDto(
                      null, toimija, kuvaus, alkoi, loppui, null, true, null, osasuoritukset));
            })
        .toList();
  }

  private static Set<String> getOsasuoritukset(JsonNode osasuoritukset) {
    return osasuoritukset == null
        ? Set.of()
        : osasuoritukset
            .valueStream()
            .map(
                node ->
                    node.path("koulutusmoduuli")
                        .path("nimi")
                        .path(Kieli.FI.getKoodi())
                        .stringValue(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  /**
   * Joins two LocalizedString instances by concatenating their values with ": " as separator.
   *
   * <p>Only the language keys present in the lhs are included in the result.
   */
  @SuppressWarnings("unchecked")
  static LocalizedString join(LocalizedString lhs, LocalizedString rhs) {

    if (lhs == null || rhs == null) {
      return lhs;
    }

    return new LocalizedString(
        Map.ofEntries(
            lhs.asMap().entrySet().stream()
                .map(
                    entry ->
                        (rhs.get(entry.getKey()) instanceof String s)
                            ? Map.entry(entry.getKey(), String.join(": ", entry.getValue(), s))
                            : entry)
                .toArray(Map.Entry[]::new)));
  }

  @SuppressWarnings("unchecked")
  private static LocalizedString getLocalizedString(JsonNode text) {
    if (text == null || text.isEmpty() || !text.isObject()) {
      return null;
    }

    var values =
        Stream.of(Kieli.values())
            .map(
                kieli ->
                    (text.path(kieli.getKoodi()).stringValue(null) instanceof String value)
                        ? Map.entry(kieli, value)
                        : null)
            .filter(Objects::nonNull)
            .toArray(Map.Entry[]::new);

    return LocalizedString.fromJsonNormalized(Map.ofEntries(values));
  }

  private static LocalDate getLocalDate(JsonNode node) {
    return node != null && node.isString() ? LocalDate.parse(node.asString()) : null;
  }

  private static final Set<OsaamisenTunnistusStatus> statuses =
      Set.of(OsaamisenTunnistusStatus.DONE, OsaamisenTunnistusStatus.FAIL);

  @Transactional(readOnly = true)
  public List<KoulutusDto> getOsaamisetIdentified(JodUser user, List<UUID> uuids) {
    return koulutusRepository
        .findByKokonaisuusYksiloIdAndIdInAndOsaamisenTunnistusStatusIn(
            user.getId(), uuids, statuses)
        .stream()
        .map(Mapper::mapKoulutus)
        .toList();
  }
}
