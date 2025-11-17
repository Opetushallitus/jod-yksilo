/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.profiili.suunnitelma.OsaamisListaDto;
import fi.okm.jod.yksilo.dto.profiili.suunnitelma.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.PolunSuunnitelmaRepository;
import fi.okm.jod.yksilo.repository.TavoiteRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolunSuunnitelmaService {
  private final TavoiteRepository tavoiteRepository;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;
  private final PolunSuunnitelmaRepository suunnitelmaRepository;
  private final OsaaminenRepository osaamisetRepository;
  private final MuuOsaaminenService muuOsaaminenService;

  @Transactional(readOnly = true)
  public PolunSuunnitelmaDto get(JodUser user, UUID tavoiteId, UUID id) {
    return suunnitelmaRepository
        .findByTavoiteYksiloIdAndTavoiteIdAndId(user.getId(), tavoiteId, id)
        .map(Mapper::mapPolunSuunnitelma)
        .orElseThrow(PolunSuunnitelmaService::notFound);
  }

  public UUID add(JodUser user, UUID tavoiteId, PolunSuunnitelmaDto dto) {
    var tavoite =
        tavoiteRepository
            .findByYksiloIdAndId(user.getId(), tavoiteId)
            .orElseThrow(PolunSuunnitelmaService::notFound);

    if (MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS.equals(tavoite.getMahdollisuusTyyppi())) {
      throw new ServiceValidationException("Invalid Tavoite");
    }

    if (suunnitelmaRepository.countByTavoite(tavoite) >= getSuunnitelmaPerTavoiteLimit()) {
      throw new ServiceValidationException("Too many Suunnitelmas");
    }

    return add(tavoite, dto).getId();
  }

  public void update(JodUser user, UUID tavoiteId, PolunSuunnitelmaDto dto) {
    var suunnitelma =
        suunnitelmaRepository
            .findByTavoiteYksiloIdAndTavoiteIdAndId(user.getId(), tavoiteId, dto.id())
            .orElseThrow(PolunSuunnitelmaService::notFound);
    update(suunnitelma, dto);
  }

  public void delete(JodUser user, UUID tavoiteId, UUID id) {
    var suunnitelma =
        suunnitelmaRepository
            .findByTavoiteYksiloIdAndTavoiteIdAndId(user.getId(), tavoiteId, id)
            .orElseThrow(PolunSuunnitelmaService::notFound);
    delete(suunnitelma);
  }

  private PolunSuunnitelma add(Tavoite tavoite, PolunSuunnitelmaDto dto) {

    var entity = new PolunSuunnitelma(tavoite);
    entity.setNimi(dto.nimi());
    if (dto.koulutusmahdollisuusId() == null) {
      var entities = getOsaamiset(dto);
      entity.setOsaamiset(entities);
      entity.setNimi(dto.nimi());
      entity.setKuvaus(dto.kuvaus());
    } else {
      final Koulutusmahdollisuus koulutusmahdollisuus =
          this.koulutusmahdollisuusRepository
              .findById(dto.koulutusmahdollisuusId())
              .orElseThrow(() -> new NotFoundException("Koulutusmahdollisuus not found"));
      entity.setKoulutusmahdollisuus(koulutusmahdollisuus);
      entity.setNimi(koulutusmahdollisuus.getOtsikko());
      entity.setKuvaus(koulutusmahdollisuus.getKuvaus());
    }
    entity = suunnitelmaRepository.save(entity);
    return entity;
  }

  private void update(PolunSuunnitelma entity, PolunSuunnitelmaDto dto) {
    entity.setNimi(dto.nimi());
    var osaamiset = dto.osaamiset();
    if (osaamiset != null) {
      var entities = getOsaamiset(dto);
      entity.setOsaamiset(entities);
      muuOsaaminenService.add(entity.getTavoite().getYksilo(), entities);
    }
    if (dto.ignoredOsaamiset() != null) {
      entity.setIgnoredOsaamiset(getIgnoredOsaamiset(entity, dto));
    }
    suunnitelmaRepository.save(entity);
  }

  private void delete(PolunSuunnitelma entity) {
    suunnitelmaRepository.delete(entity);
  }

  // palautetaan kaikki osaamiset, mitkä on dto:ssa ja tavoitteen osaamisissa
  private Set<Osaaminen> getOsaamiset(OsaamisListaDto dto) {
    var ids = dto.osaamiset();
    var osaamiset = osaamisetRepository.findByUriIn(dto.osaamiset());

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    }
    return osaamiset;
  }

  private Set<Osaaminen> getIgnoredOsaamiset(
      PolunSuunnitelma suunnitelma, PolunSuunnitelmaDto dto) {
    var ids = dto.ignoredOsaamiset();
    var osaamiset = osaamisetRepository.findByUriIn(ids);

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    } else if (!suunnitelma.getTavoite().getOsaamiset().containsAll(ids)) {
      throw new ServiceValidationException("Osaaminen not in tavoite");
    }

    return osaamiset;
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }

  static int getSuunnitelmaPerTavoiteLimit() {
    return Limits.SUUNNITELMA_PER_PAAMAARA;
  }
}
