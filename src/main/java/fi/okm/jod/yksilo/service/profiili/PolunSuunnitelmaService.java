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
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaUpdateDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.PaamaaraRepository;
import fi.okm.jod.yksilo.repository.PolunSuunnitelmaRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolunSuunnitelmaService {
  private final PaamaaraRepository paamaaraRepository;
  private final PolunSuunnitelmaRepository suunnitelmaRepository;
  private final OsaaminenRepository osaamisetRepository;
  private final MuuOsaaminenService muuOsaaminenService;

  @Transactional(readOnly = true)
  public PolunSuunnitelmaDto get(JodUser user, UUID paamaaraId, UUID id) {
    return suunnitelmaRepository
        .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, id)
        .map(Mapper::mapPolunSuunnitelma)
        .orElseThrow(PolunSuunnitelmaService::notFound);
  }

  public UUID add(JodUser user, UUID paamaaraId, PolunSuunnitelmaDto dto) {
    var paamaara =
        paamaaraRepository
            .findByYksiloIdAndId(user.getId(), paamaaraId)
            .orElseThrow(PolunSuunnitelmaService::notFound);

    if (MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS.equals(paamaara.getMahdollisuusTyyppi())) {
      throw new ServiceValidationException("Invalid Paamaara");
    }

    if (suunnitelmaRepository.countByPaamaara(paamaara) >= getSuunnitelmaPerPaamaaraLimit()) {
      throw new ServiceValidationException("Too many Suunnitelmas");
    }

    return add(paamaara, dto).getId();
  }

  public void update(JodUser user, UUID paamaaraId, PolunSuunnitelmaUpdateDto dto) {
    var suunnitelma =
        suunnitelmaRepository
            .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, dto.id())
            .orElseThrow(PolunSuunnitelmaService::notFound);
    update(suunnitelma, dto);
  }

  public void delete(JodUser user, UUID paamaaraId, UUID id) {
    var suunnitelma =
        suunnitelmaRepository
            .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, id)
            .orElseThrow(PolunSuunnitelmaService::notFound);
    delete(suunnitelma);
  }

  private PolunSuunnitelma add(Paamaara paamaara, PolunSuunnitelmaDto dto) {
    var entity = new PolunSuunnitelma(paamaara);
    entity.setNimi(dto.nimi());
    entity = suunnitelmaRepository.save(entity);
    return entity;
  }

  private void update(PolunSuunnitelma entity, PolunSuunnitelmaUpdateDto dto) {
    entity.setNimi(dto.nimi());
    var osaamiset = dto.osaamiset();
    if (osaamiset != null) {
      var entities = getOsaamiset(entity, dto);
      entity.setOsaamiset(entities);
      muuOsaaminenService.add(entity.getPaamaara().getYksilo(), entities);
    }
    if (dto.ignoredOsaamiset() != null) {
      entity.setIgnoredOsaamiset(getIgnoredOsaamiset(entity, dto));
    }
    suunnitelmaRepository.save(entity);
  }

  private void delete(PolunSuunnitelma entity) {
    suunnitelmaRepository.delete(entity);
  }

  private Set<Osaaminen> getOsaamiset(PolunSuunnitelma suunnitelma, PolunSuunnitelmaUpdateDto dto) {
    var ids = dto.osaamiset();
    var osaamiset = osaamisetRepository.findByUriIn(dto.osaamiset());
    var vaiheOsaamiset =
        suunnitelma.getVaiheet().stream()
            .flatMap(v -> v.getOsaamiset().stream())
            .collect(Collectors.toSet());

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    } else if (!suunnitelma.getPaamaara().getOsaamiset().containsAll(ids)) {
      throw new ServiceValidationException("Osaaminen not in paamaara");
    } else if (osaamiset.stream().anyMatch(vaiheOsaamiset::contains)) {
      throw new ServiceValidationException("Osaaminen in vaihe");
    }

    return osaamiset;
  }

  private Set<Osaaminen> getIgnoredOsaamiset(
      PolunSuunnitelma suunnitelma, PolunSuunnitelmaUpdateDto dto) {
    var ids = dto.ignoredOsaamiset();
    var osaamiset = osaamisetRepository.findByUriIn(ids);

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    } else if (!suunnitelma.getPaamaara().getOsaamiset().containsAll(ids)) {
      throw new ServiceValidationException("Osaaminen not in paamaara");
    }

    return osaamiset;
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }

  static int getSuunnitelmaPerPaamaaraLimit() {
    return Limits.SUUNNITELMA_PER_PAAMAARA;
  }
}
