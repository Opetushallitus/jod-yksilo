/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import static java.util.function.Function.identity;

import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaSaveDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.entity.CvTehtava;
import fi.okm.jod.yksilo.repository.CvTehtavaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceConflictException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.ProfileDeletedEvent;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import fi.okm.jod.yksilo.service.profiili.TyopaikkaService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvService {

  private final CvTehtavaRepository tehtavat;
  private final YksiloRepository yksilot;
  private final CvStorage storage;
  private final CvMessageSender sender;
  private final KoulutusKokonaisuusService koulutusKokonaisuusService;
  private final TyopaikkaService tyopaikkaService;
  private final ToimintoService toimintoService;

  @Transactional(readOnly = true)
  public void checkNoInFlightTask(JodUser user) {
    if (tehtavat.existsByYksiloAndTila(
        yksilot.getReferenceById(user.getId()), CvTehtavaTila.ODOTTAA)) {
      throw new ServiceConflictException("In-flight CV task already exists");
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public CvTehtavaDto submit(JodUser user, Kieli lang, byte[] pdf) {
    CvTehtava tehtava;
    String s3Key;

    try {
      tehtava = tehtavat.save(new CvTehtava(yksilot.getReferenceById(user.getId()), lang));
    } catch (DataIntegrityViolationException _) {
      throw new ServiceConflictException("In-flight CV task already exists");
    }

    try {
      s3Key = storage.upload(tehtava.getId(), user.getId(), pdf);
    } catch (Exception e) {
      tehtavat.updateTila(tehtava.getId(), CvTehtavaTila.EPAONNISTUNUT);
      throw new ServiceException("CV upload failed", e);
    }

    try {
      sender.send(new CvRequestMessage(tehtava.getId(), user.getId(), s3Key));
    } catch (Exception e) {
      tehtavat.updateTila(tehtava.getId(), CvTehtavaTila.EPAONNISTUNUT);
      // storage handles cleanup (e.g. S3 lifecycle rules)
      throw new ServiceException("Failed to send CV extraction request", e);
    }

    return toDto(tehtava);
  }

  @Transactional(readOnly = true)
  public CvTehtavaDto getStatus(JodUser user, UUID id) {
    return tehtavat
        .findByIdAndYksilo(id, yksilot.getReferenceById(user.getId()))
        .map(this::toDto)
        .orElseThrow(() -> new NotFoundException("Task not found"));
  }

  @Transactional
  public void save(JodUser user, UUID tehtavaId, CvTehtavaSaveDto dto) {
    var tehtava =
        tehtavat
            .findByIdAndYksilo(tehtavaId, yksilot.getReferenceById(user.getId()))
            .orElseThrow(() -> new NotFoundException("Task not found"));

    var tulos = tehtava.getTulos();
    if (tehtava.getTila() != CvTehtavaTila.VALMIS || tulos == null) {
      throw new ServiceValidationException("Invalid task status");
    }

    koulutusKokonaisuusService.add(
        user,
        filterSelected(
            dto.koulutuskokonaisuudet(),
            tulos.koulutuskokonaisuudet(),
            KoulutusKokonaisuusDto::id,
            KoulutusKokonaisuusDto::koulutukset,
            KoulutusDto::id,
            (k, filtered) -> new KoulutusKokonaisuusDto(k.id(), k.nimi(), filtered)));

    tyopaikkaService.add(
        user,
        filterSelected(
            dto.tyopaikat(),
            tulos.tyopaikat(),
            TyopaikkaDto::id,
            TyopaikkaDto::toimenkuvat,
            ToimenkuvaDto::id,
            (t, filtered) -> new TyopaikkaDto(t.id(), t.nimi(), filtered)));

    toimintoService.add(
        user,
        filterSelected(
            dto.toiminnot(),
            tulos.toiminnot(),
            ToimintoDto::id,
            ToimintoDto::patevyydet,
            PatevyysDto::id,
            (t, filtered) -> new ToimintoDto(t.id(), t.nimi(), filtered)));

    tehtavat.delete(tehtava);
  }

  @Transactional
  public void delete(JodUser user, UUID id) {
    var tehtava =
        tehtavat.findByIdAndYksilo(id, yksilot.getReferenceById(user.getId())).orElse(null);
    if (tehtava == null) {
      return;
    }
    if (tehtava.getTila() == CvTehtavaTila.ODOTTAA) {
      throw new ServiceConflictException("Cannot delete a pending CV task");
    }
    tehtavat.delete(tehtava);
  }

  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  @Transactional
  void cleanup() {
    var failed =
        tehtavat.failExpired(CvTehtavaTila.ODOTTAA, Instant.now().minus(1, ChronoUnit.HOURS));
    if (failed > 0) {
      log.info("Marked {} CV tasks as failed due to timeout", failed);
    }
    var deleted =
        tehtavat.deleteExpired(
            Set.of(CvTehtavaTila.VALMIS, CvTehtavaTila.EPAONNISTUNUT),
            Instant.now().minus(30, ChronoUnit.DAYS));
    if (deleted > 0) {
      log.info("Removed {} expired CV tasks", deleted);
    }
  }

  @EventListener(ProfileDeletedEvent.class)
  @Transactional(propagation = Propagation.MANDATORY)
  void userDeleted(ProfileDeletedEvent event) {
    log.info("Deleting CV tasks for deleted user");
    tehtavat.deleteByYksilo(yksilot.getReferenceById(event.user().getId()));
  }

  private CvTehtavaDto toDto(CvTehtava t) {
    return new CvTehtavaDto(t.getId(), t.getTila(), t.getTulos());
  }

  /**
   * Filters items based on user selections (with optional child filtering) and returns the result.
   */
  static <T, C> Set<T> filterSelected(
      List<CvTehtavaSaveDto.Valinta> selections,
      Collection<T> items,
      Function<T, UUID> getId,
      Function<T, Set<C>> getChildren,
      Function<C, UUID> getChildId,
      BiFunction<T, Set<C>, T> withFilteredChildren) {

    if (selections == null || items == null) {
      return Set.of();
    }

    var index =
        selections.stream()
            .collect(Collectors.toMap(CvTehtavaSaveDto.Valinta::id, identity(), (a, _) -> a));

    return items.stream()
        .filter(item -> index.containsKey(getId.apply(item)))
        .flatMap(
            item -> {
              var selection = index.get(getId.apply(item));
              var children = getChildren.apply(item);
              if (selection.lapset() != null && children != null) {
                var filtered =
                    children.stream()
                        .filter(child -> selection.lapset().contains(getChildId.apply(child)))
                        .collect(Collectors.toSet());
                return filtered.isEmpty()
                    ? Stream.empty()
                    : Stream.of(withFilteredChildren.apply(item, filtered));
              }
              return Stream.empty();
            })
        .collect(Collectors.toSet());
  }
}
