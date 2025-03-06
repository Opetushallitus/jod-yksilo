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
import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaUpdateDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.PaamaaraRepository;
import fi.okm.jod.yksilo.repository.PolunSuunnitelmaRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolunSuunnitelmaService {
  private final PaamaaraRepository paamaarat;
  private final PolunSuunnitelmaRepository suunnitelmat;
  private final YksilonOsaaminenService osaamiset;
  private final OsaaminenRepository osaamisetRepository;
  private final YksilonOsaaminenRepository yksilonOsaaminenRepository;

  @Transactional(readOnly = true)
  public PolunSuunnitelmaDto get(JodUser user, UUID paamaaraId, UUID id) {
    return suunnitelmat
        .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, id)
        .map(Mapper::mapPolunSuunnitelma)
        .orElseThrow(PolunSuunnitelmaService::notFound);
  }

  public UUID add(JodUser user, UUID paamaaraId, PolunSuunnitelmaDto dto) {
    var paamaara =
        paamaarat
            .findByYksiloIdAndId(user.getId(), paamaaraId)
            .orElseThrow(PolunSuunnitelmaService::notFound);

    if (suunnitelmat.countByPaamaara(paamaara) >= getSuunnitelmaPerPaamaaraLimit()) {
      throw new ServiceValidationException("Too many Suunnitelmas");
    }

    return add(paamaara, dto).getId();
  }

  public void update(JodUser user, UUID paamaaraId, PolunSuunnitelmaUpdateDto dto) {
    var suunnitelma =
        suunnitelmat
            .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, dto.id())
            .orElseThrow(PolunSuunnitelmaService::notFound);
    update(suunnitelma, dto);
  }

  public void delete(JodUser user, UUID paamaaraId, UUID id) {
    var suunnitelma =
        suunnitelmat
            .findByPaamaaraYksiloIdAndPaamaaraIdAndId(user.getId(), paamaaraId, id)
            .orElseThrow(PolunSuunnitelmaService::notFound);
    delete(suunnitelma);
  }

  private PolunSuunnitelma add(Paamaara paamaara, PolunSuunnitelmaDto dto) {
    var entity = new PolunSuunnitelma(paamaara);
    entity.setNimi(dto.nimi());
    entity = suunnitelmat.save(entity);
    return entity;
  }

  private void update(PolunSuunnitelma entity, PolunSuunnitelmaUpdateDto dto) {
    entity.setNimi(dto.nimi());
    if (dto.osaamiset() != null) {
      var yksilo = entity.getPaamaara().getYksilo();
      osaamiset.add(
          new MuuOsaaminen(
              yksilo,
              new HashSet<>(
                  yksilonOsaaminenRepository.findAllByYksiloIdAndLahde(
                      yksilo.getId(), OsaamisenLahdeTyyppi.MUU_OSAAMINEN, Sort.unsorted()))),
          dto.osaamiset());
      entity.setOsaamiset(getOsaamiset(entity, dto));
    }
    if (dto.alaHuomioiOsaamiset() != null) {
      entity.setAlaHuomioiOsaamiset(getAlaHuomioiOsaamiset(entity, dto));
    }
    suunnitelmat.save(entity);
  }

  private void delete(PolunSuunnitelma entity) {
    suunnitelmat.delete(entity);
  }

  private List<Osaaminen> getOsaamiset(
      PolunSuunnitelma suunnitelma, PolunSuunnitelmaUpdateDto dto) {
    var ids =
        dto.osaamiset().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet());
    var osaamiset = osaamisetRepository.findByUriIn(ids);
    var vaiheOsaamiset =
        suunnitelma.getVaiheet().stream()
            .flatMap(v -> v.getOsaamiset().stream())
            .map(Osaaminen::getUri)
            .map(Objects::toString)
            .collect(Collectors.toSet());

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    } else if (!suunnitelma.getPaamaara().getOsaamiset().containsAll(ids)) {
      throw new ServiceValidationException("Osaaminen not in paamaara");
    } else if (ids.stream().anyMatch(vaiheOsaamiset::contains)) {
      throw new ServiceValidationException("Osaaminen in vaihe");
    }

    return osaamiset;
  }

  private List<Osaaminen> getAlaHuomioiOsaamiset(
      PolunSuunnitelma suunnitelma, PolunSuunnitelmaUpdateDto dto) {
    var ids =
        dto.alaHuomioiOsaamiset().stream()
            .map(Object::toString)
            .collect(Collectors.toUnmodifiableSet());
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
