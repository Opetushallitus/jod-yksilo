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
import fi.okm.jod.yksilo.dto.profiili.PolunVaiheDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.PolunVaihe;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.PolunSuunnitelmaRepository;
import fi.okm.jod.yksilo.repository.PolunVaiheRepository;
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
public class PolunVaiheService {
  private final OsaaminenRepository osaamisetRepository;
  private final PolunSuunnitelmaRepository suunnitelmaRepository;
  private final PolunVaiheRepository vaiheRepository;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;

  public UUID add(JodUser user, UUID tavoiteId, UUID suunnitelmaId, PolunVaiheDto dto) {
    var suunnitelma =
        suunnitelmaRepository
            .findByTavoiteYksiloIdAndTavoiteIdAndId(user.getId(), tavoiteId, suunnitelmaId)
            .orElseThrow(PolunVaiheService::notFound);

    var koulutusmahdollisuus =
        dto.mahdollisuusId() != null
            ? koulutusmahdollisuusRepository.findById(dto.mahdollisuusId()).orElse(null)
            : null;

    if (vaiheRepository.countByPolunSuunnitelma(suunnitelma) >= getVaihePerSuunnitelmaLimit()) {
      throw new ServiceValidationException("Too many Vaihes");
    }

    return add(suunnitelma, dto, koulutusmahdollisuus).getId();
  }

  public void update(JodUser user, UUID tavoiteId, UUID suunnitelmaId, PolunVaiheDto dto) {
    var vaihe =
        vaiheRepository
            .findByPolunSuunnitelmaTavoiteYksiloIdAndPolunSuunnitelmaTavoiteIdAndPolunSuunnitelmaIdAndId(
                user.getId(), tavoiteId, suunnitelmaId, dto.id())
            .orElseThrow(PolunVaiheService::notFound);
    update(vaihe, dto);
  }

  public void delete(JodUser user, UUID tavoiteId, UUID suunnitelmaId, UUID vaiheId) {
    var vaihe =
        vaiheRepository
            .findByPolunSuunnitelmaTavoiteYksiloIdAndPolunSuunnitelmaTavoiteIdAndPolunSuunnitelmaIdAndId(
                user.getId(), tavoiteId, suunnitelmaId, vaiheId)
            .orElseThrow(PolunVaiheService::notFound);
    delete(vaihe);
  }

  private PolunVaihe add(
      PolunSuunnitelma polunSuunnitelma,
      PolunVaiheDto dto,
      Koulutusmahdollisuus koulutusmahdollisuus) {
    var entity = new PolunVaihe(polunSuunnitelma, koulutusmahdollisuus);
    updateEntityFieldFromDtoData(entity, dto);
    entity = vaiheRepository.save(entity);

    return entity;
  }

  private void update(PolunVaihe entity, PolunVaiheDto dto) {
    updateEntityFieldFromDtoData(entity, dto);
    vaiheRepository.save(entity);
  }

  private void updateEntityFieldFromDtoData(PolunVaihe entity, PolunVaiheDto dto) {
    entity.setLahde(dto.lahde());
    entity.setTyyppi(dto.tyyppi());
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setLinkit(dto.linkit());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    if (dto.osaamiset() != null) {
      entity.setOsaamiset(getOsaamiset(entity, dto));
    }
    entity.setValmis(dto.valmis());
  }

  private void delete(PolunVaihe entity) {
    vaiheRepository.delete(entity);
  }

  private Set<Osaaminen> getOsaamiset(PolunVaihe vaihe, PolunVaiheDto dto) {
    var ids = dto.osaamiset();
    var osaamiset = osaamisetRepository.findByUriIn(ids);
    var suunnitelma = vaihe.getPolunSuunnitelma();
    var tavoiteOsaamiset = suunnitelma.getTavoite().getOsaamiset();
    var suunnitelmaOsaamiset =
        suunnitelma.getOsaamiset().stream().map(Osaaminen::getUri).collect(Collectors.toSet());
    var vaiheet = suunnitelma.getVaiheet();
    var vaiheetOsaamiset =
        vaiheet.stream()
            .filter(v -> v.getId() != vaihe.getId())
            .flatMap(v -> v.getOsaamiset().stream())
            .map(Osaaminen::getUri)
            .collect(Collectors.toSet());

    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    } else if (!tavoiteOsaamiset.containsAll(ids)) {
      throw new ServiceValidationException("Osaaminen not in tavoite");
    } else if (ids.stream().anyMatch(suunnitelmaOsaamiset::contains)) {
      throw new ServiceValidationException("Osaaminen in suunnitelma");
    } else if (ids.stream().anyMatch(vaiheetOsaamiset::contains)) {
      throw new ServiceValidationException("Osaaminen in another vaihe");
    }

    return osaamiset;
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }

  static int getVaihePerSuunnitelmaLimit() {
    return Limits.VAIHE_PER_SUUNNITELMA;
  }
}
