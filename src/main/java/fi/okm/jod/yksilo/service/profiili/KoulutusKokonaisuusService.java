/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusUpdateDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.event.OsaamisetTunnistusEvent;
import fi.okm.jod.yksilo.repository.KoulutusKokonaisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KoulutusKokonaisuusService {

  private final YksiloRepository yksilot;
  private final KoulutusKokonaisuusRepository kokonaisuudet;
  private final KoulutusService koulutusService;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional(readOnly = true)
  public List<KoulutusKokonaisuusDto> findAll(JodUser user) {
    return kokonaisuudet.findByYksiloId(user.getId()).stream()
        .map(Mapper::mapKoulutusKokonaisuus)
        .toList();
  }

  public List<UUID> addManyForImport(JodUser user, Set<KoulutusKokonaisuusDto> dtos) {
    var koulutukset = add(user, dtos, true).flatMap(dto -> dto.getKoulutukset().stream()).toList();
    applicationEventPublisher.publishEvent(new OsaamisetTunnistusEvent(user, koulutukset));
    return koulutukset.stream().map(Koulutus::getId).toList();
  }

  public UUID add(JodUser user, KoulutusKokonaisuusDto dto) {
    return add(user, Set.of(dto)).getFirst();
  }

  public SequencedSet<UUID> add(JodUser user, Set<KoulutusKokonaisuusDto> dtos) {
    return add(user, dtos, false)
        .map(KoulutusKokonaisuus::getId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Stream<KoulutusKokonaisuus> add(
      JodUser user, Set<KoulutusKokonaisuusDto> dtos, boolean tunnistaOsaamiset) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (kokonaisuudet.countByYksilo(yksilo) + dtos.size() > Limits.KOULUTUSKOKONAISUUS) {
      throw new ServiceValidationException("Too many KoulutusKokonaisuus");
    }

    return dtos.stream()
        .map(
            dto -> {
              var entity = kokonaisuudet.save(new KoulutusKokonaisuus(yksilo, dto.nimi()));
              for (var koulutus : dto.koulutukset()) {
                entity
                    .getKoulutukset()
                    .add(koulutusService.add(entity, koulutus, tunnistaOsaamiset));
              }
              return entity;
            });
  }

  @Transactional(readOnly = true)
  public KoulutusKokonaisuusDto get(JodUser user, UUID id) {
    return kokonaisuudet
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapKoulutusKokonaisuus)
        .orElseThrow(KoulutusKokonaisuusService::notFound);
  }

  public void update(JodUser user, KoulutusKokonaisuusUpdateDto dto) {
    var entity =
        kokonaisuudet
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(KoulutusKokonaisuusService::notFound);
    entity.setNimi(dto.nimi());
    kokonaisuudet.save(entity);
  }

  public void delete(JodUser user, UUID id) {
    var kokonaisuus =
        kokonaisuudet
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(KoulutusKokonaisuusService::notFound);
    for (var koulutus : kokonaisuus.getKoulutukset()) {
      koulutusService.delete(koulutus);
    }
    kokonaisuudet.delete(kokonaisuus);
  }

  private static ServiceException notFound() {
    return new NotFoundException("KoulutusKokonaisuus not found");
  }
}
