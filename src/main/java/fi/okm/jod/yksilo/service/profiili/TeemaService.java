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
import fi.okm.jod.yksilo.dto.profiili.TeemaDto;
import fi.okm.jod.yksilo.dto.profiili.TeemaUpdateDto;
import fi.okm.jod.yksilo.entity.Teema;
import fi.okm.jod.yksilo.repository.TeemaRepository;
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
public class TeemaService {

  private final YksiloRepository yksilot;
  private final TeemaRepository teemat;
  private final PatevyysService patevyysService;

  @Transactional(readOnly = true)
  public List<TeemaDto> findAll(JodUser user) {
    return teemat.findByYksiloId(user.getId()).stream().map(Mapper::mapTeema).toList();
  }

  @Transactional(readOnly = true)
  public TeemaDto get(JodUser user, UUID id) {
    return teemat
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapTeema)
        .orElseThrow(KoulutusService::notFound);
  }

  public UUID add(JodUser user, TeemaDto dto) {
    // tuontiLahde is system-controlled metadata
    var sanitized = new TeemaDto(dto.id(), dto.nimi(), null, dto.patevyydet());
    return addFromImport(user, Set.of(sanitized)).getFirst();
  }

  public SequencedSet<UUID> addFromImport(JodUser user, Set<TeemaDto> dtos) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (teemat.countByYksilo(yksilo) + dtos.size() > Limits.TEEMA) {
      throw new ProfileLimitException(ProfileItem.TEEMA);
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
              var teema = new Teema(yksilo, dto.nimi());
              teema.setTuontiLahde(dto.tuontiLahde());
              teema = teemat.save(teema);
              if (dto.patevyydet() != null) {
                for (var patevyys : dto.patevyydet()) {
                  teema.getPatevyydet().add(patevyysService.add(teema, patevyys));
                }
              }
              return teema.getId();
            })
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public void update(JodUser user, TeemaUpdateDto dto) {
    var teema =
        teemat
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Teema not found"));
    teema.setNimi(dto.nimi());
    teema.setTuontiLahde(null);
    teemat.flush();
  }

  public void delete(JodUser user, UUID id) {
    var teema =
        teemat
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(() -> new NotFoundException("Teema not found"));
    for (var patevyys : teema.getPatevyydet()) {
      patevyysService.delete(patevyys);
    }
    teemat.delete(teema);
  }
}
