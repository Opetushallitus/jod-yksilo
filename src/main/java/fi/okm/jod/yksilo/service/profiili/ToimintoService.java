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
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoUpdateDto;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.profiili.ProfileLimitException.ProfileItem;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ToimintoService {

  private final YksiloRepository yksilot;
  private final ToimintoRepository toiminnot;
  private final PatevyysService patevyysService;

  @Transactional(readOnly = true)
  public List<ToimintoDto> findAll(JodUser user) {
    return toiminnot.findByYksiloId(user.getId()).stream().map(Mapper::mapToiminto).toList();
  }

  @Transactional(readOnly = true)
  public ToimintoDto get(JodUser user, UUID id) {
    return toiminnot
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapToiminto)
        .orElseThrow(KoulutusService::notFound);
  }

  public UUID add(JodUser user, ToimintoDto dto) {
    return add(user, Set.of(dto)).getFirst();
  }

  public SequencedSet<UUID> add(JodUser user, Set<ToimintoDto> dtos) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (toiminnot.countByYksilo(yksilo) + dtos.size() > Limits.TOIMINTO) {
      throw new ProfileLimitException(ProfileItem.TOIMINTO);
    }

    var count =
        dtos.stream()
            .map(t -> t.patevyydet() == null ? 0 : t.patevyydet().size())
            .reduce(0, Integer::sum);

    if (patevyysService.countBy(yksilo) + count > Limits.PATEVYYS) {
      throw new ProfileLimitException(ProfileItem.PATEVYYS);
    }

    return dtos.stream()
        .map(
            dto -> {
              var toiminto = toiminnot.save(new Toiminto(yksilo, dto.nimi()));
              if (dto.patevyydet() != null) {
                for (var patevyys : dto.patevyydet()) {
                  toiminto.getPatevyydet().add(patevyysService.add(toiminto, patevyys));
                }
              }
              return toiminto.getId();
            })
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public void update(JodUser user, ToimintoUpdateDto dto) {
    var toiminto =
        toiminnot
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Toiminto not found"));
    toiminto.setNimi(dto.nimi());
    toiminnot.flush();
  }

  public void delete(JodUser user, UUID id) {
    var toiminto =
        toiminnot
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(() -> new NotFoundException("Toiminto not found"));
    for (var patevyys : toiminto.getPatevyydet()) {
      patevyysService.delete(patevyys);
    }
    toiminnot.delete(toiminto);
  }
}
