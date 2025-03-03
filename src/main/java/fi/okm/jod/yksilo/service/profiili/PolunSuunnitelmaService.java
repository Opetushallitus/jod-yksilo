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
import fi.okm.jod.yksilo.dto.profiili.PolunSuunnitelmaDto;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.repository.PaamaaraRepository;
import fi.okm.jod.yksilo.repository.PolunSuunnitelmaRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolunSuunnitelmaService {
  private final PaamaaraRepository paamaarat;
  private final PolunSuunnitelmaRepository suunnitelmat;

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

  public void update(JodUser user, UUID paamaaraId, PolunSuunnitelmaDto dto) {
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

  private void update(PolunSuunnitelma entity, PolunSuunnitelmaDto dto) {
    entity.setNimi(dto.nimi());
    suunnitelmat.save(entity);
  }

  private void delete(PolunSuunnitelma entity) {
    suunnitelmat.delete(entity);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }

  static int getSuunnitelmaPerPaamaaraLimit() {
    return Limits.SUUNNITELMA_PER_PAAMAARA;
  }
}
