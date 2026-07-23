/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static java.util.function.Function.identity;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.entity.KoskiTehtava;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.repository.KoskiTehtavaRepository;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.Mapper;
import fi.okm.jod.yksilo.service.profiili.ProfileDeletedEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@ConditionalOnBean(KoskiOauth2Config.class)
@Service
@Slf4j
public class KoskiService {

  private final KoulutusRepository koulutusRepository;
  private final KoskiTehtavaRepository tehtavat;
  private final YksiloRepository yksilot;
  private final KoulutusKokonaisuusService koulutusKokonaisuusService;

  public KoskiService(
      KoulutusRepository koulutusRepository,
      KoskiTehtavaRepository tehtavat,
      YksiloRepository yksilot,
      KoulutusKokonaisuusService koulutusKokonaisuusService) {
    this.koulutusRepository = koulutusRepository;
    this.tehtavat = tehtavat;
    this.yksilot = yksilot;
    this.koulutusKokonaisuusService = koulutusKokonaisuusService;
    log.info("Creating KoskiService");
  }

  public List<KoulutusKokonaisuusDto> mapKoulutusKokonaisuudet(JsonNode koskiResponse) {
    return streamOpiskeluoikeudet(koskiResponse)
        .map(
            m -> {
              // KoulutusDto.nimi is required; fall back to institution when degree name is missing
              var koulutusNimi = m.kuvaus() != null ? m.kuvaus() : m.toimija();
              var koulutus =
                  new KoulutusDto(
                      UUID.randomUUID(),
                      koulutusNimi,
                      null,
                      m.alkoi(),
                      m.loppui(),
                      null,
                      true,
                      null,
                      m.osasuoritukset());
              return new KoulutusKokonaisuusDto(
                  UUID.randomUUID(), m.toimija(), TuontiLahde.KOSKI_TUONTI, Set.of(koulutus));
            })
        .toList();
  }

  private record OpiskeluoikeusMapping(
      LocalizedString toimija,
      LocalizedString kuvaus,
      LocalDate alkoi,
      LocalDate loppui,
      Set<String> osasuoritukset) {}

  private Stream<OpiskeluoikeusMapping> streamOpiskeluoikeudet(JsonNode koskiResponse) {
    if (koskiResponse == null) {
      return Stream.of();
    }

    return koskiResponse
        .path("opiskeluoikeudet")
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
                  new OpiskeluoikeusMapping(toimija, kuvaus, alkoi, loppui, osasuoritukset));
            });
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

  @Transactional
  public KoskiTehtavaDto submit(JodUser user, List<KoulutusKokonaisuusDto> data) {
    var tulos = new KoskiTehtavaDto.Tulos(data == null ? List.of() : data);
    var tehtava = tehtavat.save(new KoskiTehtava(yksilot.getReferenceById(user.getId()), tulos));
    return toDto(tehtava);
  }

  @Transactional(readOnly = true)
  public KoskiTehtavaDto getStatus(JodUser user, UUID id) {
    return tehtavat
        .findByIdAndYksilo(id, yksilot.getReferenceById(user.getId()))
        .map(this::toDto)
        .orElseThrow(() -> new NotFoundException("Task not found"));
  }

  @Transactional
  public List<UUID> save(JodUser user, UUID tehtavaId, KoskiTehtavaSaveDto dto) {
    var tehtava =
        tehtavat
            .findByIdAndYksilo(tehtavaId, yksilot.getReferenceById(user.getId()))
            .orElseThrow(() -> new NotFoundException("Task not found"));

    var tulos = tehtava.getTulos();
    if (tehtava.getTila() != KoskiTehtavaTila.VALMIS || tulos == null) {
      throw new ServiceValidationException("Invalid task status");
    }

    var selected = filterSelected(dto.koulutuskokonaisuudet(), tulos.koulutuskokonaisuudet());
    if (selected.isEmpty()) {
      throw new ServiceValidationException("No selections match the task result");
    }

    var ids =
        koulutusKokonaisuusService.addManyForImport(user, selected, dto.skipOsaamistenTunnistus());

    tehtava.setTila(KoskiTehtavaTila.POISTETTU);
    tehtava.setTulos(null);
    return ids;
  }

  @Transactional
  public void delete(JodUser user, UUID id) {
    var tehtava =
        tehtavat.findByIdAndYksilo(id, yksilot.getReferenceById(user.getId())).orElse(null);
    if (tehtava == null) {
      return;
    }
    tehtava.setTulos(null);
    tehtava.setTila(KoskiTehtavaTila.POISTETTU);
  }

  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  @Transactional
  void cleanup() {
    var deleted =
        tehtavat.deleteExpired(
            Set.of(
                KoskiTehtavaTila.VALMIS,
                KoskiTehtavaTila.EPAONNISTUNUT,
                KoskiTehtavaTila.POISTETTU),
            Instant.now().minus(1, ChronoUnit.DAYS));
    if (deleted > 0) {
      log.info("Removed {} expired Koski tasks", deleted);
    }
  }

  @EventListener(ProfileDeletedEvent.class)
  @Transactional(propagation = Propagation.MANDATORY)
  void userDeleted(ProfileDeletedEvent event) {
    log.info("Deleting Koski tasks for deleted user");
    tehtavat.deleteByYksilo(yksilot.getReferenceById(event.user().getId()));
  }

  private KoskiTehtavaDto toDto(KoskiTehtava t) {
    return new KoskiTehtavaDto(t.getId(), t.getTila(), t.getTulos());
  }

  static Set<KoulutusKokonaisuusDto> filterSelected(
      List<KoskiTehtavaSaveDto.Valinta> selections, List<KoulutusKokonaisuusDto> items) {

    if (selections == null || items == null) {
      return Set.of();
    }

    var index =
        selections.stream()
            .collect(Collectors.toMap(KoskiTehtavaSaveDto.Valinta::id, identity(), (a, _) -> a));

    return items.stream()
        .filter(item -> index.containsKey(item.id()))
        .map(
            item -> {
              var selection = index.get(item.id());
              var children = item.koulutukset();
              if (selection.lapset() != null && children != null) {
                var filtered =
                    children.stream()
                        .filter(child -> selection.lapset().contains(child.id()))
                        .map(
                            child ->
                                new KoulutusDto(
                                    null,
                                    child.nimi(),
                                    child.kuvaus(),
                                    child.alkuPvm(),
                                    child.loppuPvm(),
                                    child.osaamiset(),
                                    child.osaamisetOdottaaTunnistusta(),
                                    child.osaamisetTunnistusEpaonnistui(),
                                    child.osasuoritukset()))
                        .collect(Collectors.toSet());
                if (!filtered.isEmpty()) {
                  return new KoulutusKokonaisuusDto(
                      null, item.nimi(), item.tuontiLahde(), filtered);
                }
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
