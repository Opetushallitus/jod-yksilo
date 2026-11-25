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

import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureConfiguration;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto.Lapsi;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.entity.KoskiTehtava;
import fi.okm.jod.yksilo.repository.KoskiTehtavaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceConflictException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.ProfileDeletedEvent;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ConditionalOnBean(KoskiOauth2Config.class)
@Service
@Slf4j
public class KoskiService {

  private final KoskiTehtavaRepository tehtavat;
  private final YksiloRepository yksilot;
  private final KoulutusKokonaisuusService koulutusKokonaisuusService;
  private final KoskiReader reader;
  private final KoskiClient koskiClient;

  /** Absent when no extraction service is configured. */
  private final KoskiExtractionSender sender;

  private final FeatureConfiguration features;
  private final JsonMapper jsonMapper;

  /**
   * A waiting task opened after this long completes without extracted skills. Until opened, it
   * still accepts a late extraction result.
   */
  private final Duration extractionTimeout;

  /** Larger requests than this are imported without skills; SQS takes up to 1 MiB. */
  private final long maxExtractionRequestBytes;

  private static final Set<KoskiTehtavaTila> ACTIVE =
      Set.of(KoskiTehtavaTila.ODOTTAA, KoskiTehtavaTila.VALMIS);

  KoskiService(
      KoskiTehtavaRepository tehtavat,
      YksiloRepository yksilot,
      KoulutusKokonaisuusService koulutusKokonaisuusService,
      KoskiReader reader,
      KoskiClient koskiClient,
      ObjectProvider<KoskiExtractionSender> sender,
      FeatureConfiguration features,
      JsonMapper jsonMapper,
      @Value("${jod.koski.extraction.timeout:PT1H}") Duration extractionTimeout,
      @Value("${jod.koski.extraction.max-request-bytes:900000}") long maxExtractionRequestBytes) {
    this.tehtavat = tehtavat;
    this.yksilot = yksilot;
    this.koulutusKokonaisuusService = koulutusKokonaisuusService;
    this.reader = reader;
    this.koskiClient = koskiClient;
    this.sender = sender.getIfAvailable();
    this.features = features;
    this.jsonMapper = jsonMapper;
    this.extractionTimeout = extractionTimeout;
    this.maxExtractionRequestBytes = maxExtractionRequestBytes;
    log.info("Creating KoskiService (skill extraction configured: {})", this.sender != null);
  }

  /**
   * Imports the user's educations from Koski into a new import task. While a task is still being
   * processed, that task is returned instead and Koski is not called.
   *
   * <p>Not transactional: Koski and the extraction service are called outside a transaction.
   */
  public KoskiTehtavaDto startImport(JodUser user, OAuth2AccessToken accessToken) {
    var pending = findPending(user);
    return pending
        .map(this::toDto)
        .orElseGet(() -> submit(user, koskiClient.fetch(user, accessToken)));
  }

  /**
   * Creates an import task from a Koski response, replacing the user's unfinished ones. With skill
   * extraction enabled the task waits for the extraction service; otherwise, or if the request
   * cannot be sent, the educations are imported without skills.
   */
  KoskiTehtavaDto submit(JodUser user, JsonNode koskiResponse) {
    tehtavat.discard(user.getId(), ACTIVE);
    var yksilo = yksilot.getReferenceById(user.getId());
    var educations = reader.read(koskiResponse);
    var withoutSkills = new KoskiTehtavaDto.Tulos(educations.koulutuskokonaisuudet());
    if (!isExtractionEnabled() || educations.extraction().isEmpty()) {
      return toDto(tehtavat.save(new KoskiTehtava(yksilo, withoutSkills)));
    }
    var tehtava = KoskiTehtava.waiting(yksilo, withoutSkills);
    var request =
        new KoskiExtractionRequest(tehtava.getId(), user.getId(), educations.extraction());
    var size = jsonMapper.writeValueAsBytes(request).length;
    if (size > maxExtractionRequestBytes) {
      log.warn("Koski extraction request of {} bytes is too large, importing without skills", size);
      return toDto(tehtavat.save(new KoskiTehtava(yksilo, withoutSkills)));
    }
    try {
      tehtava = tehtavat.saveAndFlush(tehtava);
    } catch (DataIntegrityViolationException _) {
      throw new ServiceConflictException("Koski import task already in progress");
    }
    try {
      sender.send(request);
    } catch (Exception e) {
      log.error(
          "Sending Koski extraction request failed for task {}, importing without skills",
          tehtava.getId(),
          e);
      tehtava.finish();
      tehtava = tehtavat.save(tehtava);
    }
    return toDto(tehtava);
  }

  @Transactional
  public KoskiTehtavaDto getStatus(JodUser user, UUID id) {
    return tehtavat
        .findByIdAndYksilo(id, yksilot.getReferenceById(user.getId()))
        .map(this::finishIfTimedOut)
        .map(this::toDto)
        .orElseThrow(() -> new NotFoundException("Task not found"));
  }

  /** The user's waiting or finished but unsaved import tasks, newest first, without results. */
  @Transactional(readOnly = true)
  public List<KoskiTehtavaDto> findUnfinished(JodUser user) {
    return tehtavat
        .findByYksiloAndTilaInOrderByLuotuDesc(yksilot.getReferenceById(user.getId()), ACTIVE)
        .stream()
        .map(t -> new KoskiTehtavaDto(t.getId(), t.getTila(), null))
        .toList();
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

    var ids = koulutusKokonaisuusService.addManyForImport(user, selected);

    discard(tehtava);
    return ids;
  }

  @Transactional
  public void delete(JodUser user, UUID id) {
    var tehtava =
        tehtavat.findByIdAndYksilo(id, yksilot.getReferenceById(user.getId())).orElse(null);
    if (tehtava != null) {
      discard(tehtava);
    }
  }

  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  @Transactional
  void cleanup() {
    var deleted =
        tehtavat.deleteExpired(
            Set.of(
                KoskiTehtavaTila.ODOTTAA,
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
    // A waiting task holds the educations without skills, not yet its result.
    var tulos = t.getTila() == KoskiTehtavaTila.VALMIS ? t.getTulos() : null;
    return new KoskiTehtavaDto(t.getId(), t.getTila(), tulos);
  }

  private boolean isExtractionEnabled() {
    return sender != null && features.isFeatureEnabled(Feature.KOSKI_EXTRACTION);
  }

  /** The user's task still waiting for skill extraction, if it has not timed out. */
  private Optional<KoskiTehtava> findPending(JodUser user) {
    var pending =
        tehtavat.findByYksiloIdAndTilaAndLuotuAfter(
            user.getId(), KoskiTehtavaTila.ODOTTAA, Instant.now().minus(extractionTimeout));
    pending.ifPresent(
        t -> log.info("Koski import task {} is still being processed, continuing it", t.getId()));
    return pending;
  }

  private boolean isTimedOut(KoskiTehtava t) {
    return t.getLuotu().isBefore(Instant.now().minus(extractionTimeout));
  }

  private KoskiTehtava finishIfTimedOut(KoskiTehtava t) {
    if (t.getTila() == KoskiTehtavaTila.ODOTTAA && isTimedOut(t)) {
      log.warn(
          "Koski skill extraction for task {} timed out, continuing without skills", t.getId());
      t.finish();
    }
    return t;
  }

  private static void discard(KoskiTehtava t) {
    t.setTulos(null);
    t.setTila(KoskiTehtavaTila.POISTETTU);
  }

  /** The offered osaamiset that were also selected. */
  private static Set<URI> selectedOsaamiset(Set<URI> offered, Set<URI> selected) {
    if (offered == null || selected == null) {
      return Set.of();
    }
    return offered.stream()
        .filter(selected::contains)
        .collect(Collectors.toCollection(LinkedHashSet::new));
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
                var selectedChildren =
                    selection.lapset().stream()
                        .collect(Collectors.toMap(Lapsi::id, identity(), (a, _) -> a));
                var filtered =
                    children.stream()
                        .filter(child -> selectedChildren.containsKey(child.id()))
                        .map(
                            child ->
                                new KoulutusDto(
                                    null,
                                    child.nimi(),
                                    child.kuvaus(),
                                    child.alkuPvm(),
                                    child.loppuPvm(),
                                    selectedOsaamiset(
                                        child.osaamiset(),
                                        selectedChildren.get(child.id()).osaamiset())))
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
