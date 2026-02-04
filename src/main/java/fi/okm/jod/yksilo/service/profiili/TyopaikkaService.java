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
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaUpdateDto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
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
public class TyopaikkaService {

  private final YksiloRepository yksilot;
  private final TyopaikkaRepository tyopaikat;
  private final ToimenkuvaService toimenkuvaService;

  @Transactional(readOnly = true)
  public List<TyopaikkaDto> findAll(JodUser user) {
    return tyopaikat.findByYksiloId(user.getId()).stream().map(Mapper::mapTyopaikka).toList();
  }

  @Transactional(readOnly = true)
  public TyopaikkaDto get(JodUser user, UUID id) {
    return tyopaikat
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapTyopaikka)
        .orElseThrow(TyopaikkaService::notFound);
  }

  public void delete(JodUser user, UUID id) {
    var tyopaikka =
        tyopaikat.findByYksiloIdAndId(user.getId(), id).orElseThrow(TyopaikkaService::notFound);
    for (var toimenkuva : tyopaikka.getToimenkuvat()) {
      toimenkuvaService.delete(toimenkuva);
    }
    tyopaikat.delete(tyopaikka);
  }

  public void update(JodUser user, TyopaikkaUpdateDto dto) {
    var entity =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(TyopaikkaService::notFound);
    entity.setNimi(dto.nimi());
    tyopaikat.save(entity);
  }

  public UUID add(JodUser user, TyopaikkaDto dto) {
    return add(user, Set.of(dto)).getFirst();
  }

  public SequencedSet<UUID> add(JodUser user, Set<TyopaikkaDto> dtos) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (tyopaikat.countByYksilo(yksilo) + dtos.size() > Limits.TYOPAIKKA) {
      throw new ProfileLimitException(ProfileItem.TYOPAIKKA);
    }

    var count =
        dtos.stream()
            .map(t -> t.toimenkuvat() == null ? 0 : t.toimenkuvat().size())
            .reduce(0, Integer::sum);

    if (toimenkuvaService.countBy(yksilo) + count > Limits.TOIMENKUVA) {
      throw new ProfileLimitException(ProfileItem.TOIMENKUVA);
    }

    return dtos.stream()
        .map(
            dto -> {
              var entity = tyopaikat.save(new Tyopaikka(yksilo, dto.nimi()));
              if (dto.toimenkuvat() != null) {
                for (var toimenkuva : dto.toimenkuvat()) {
                  entity.getToimenkuvat().add(toimenkuvaService.add(entity, toimenkuva));
                }
              }
              return entity.getId();
            })
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static ServiceException notFound() {
    return new NotFoundException("Työpaikka not found");
  }
}
