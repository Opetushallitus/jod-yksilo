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
import fi.okm.jod.yksilo.domain.OsaamisenLahde;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen_;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.OsaamisenLahdeRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.ProfileLimitException.ProfileItem;
import fi.okm.jod.yksilo.validation.Limits;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class YksilonOsaaminenService {

  private final YksilonOsaaminenRepository repository;
  private final YksiloRepository yksiloRepository;
  private final OsaaminenRepository osaamisetRepository;
  private final List<OsaamisenLahdeRepository<?>> lahteet;

  @Transactional(readOnly = true)
  public List<YksilonOsaaminenDto> findAll(JodUser user, OsaamisenLahdeTyyppi tyyppi, UUID id) {

    if (tyyppi != null && id != null) {
      return lahteet.stream()
          .flatMap(s -> s.findBy(user, new OsaamisenLahdeDto(tyyppi, Optional.of(id))).stream())
          .findFirst()
          .map(l -> Mapper.mapYksilonOsaaminen(l.getOsaamiset()))
          .orElse(List.of());
    }

    var sort =
        Sort.by(
            YksilonOsaaminen_.LAHDE,
            YksilonOsaaminen_.TOIMENKUVA,
            YksilonOsaaminen_.KOULUTUS,
            YksilonOsaaminen_.ID);
    return Mapper.mapYksilonOsaaminen(repository.findAllBy(user.getId(), tyyppi, sort));
  }

  @Transactional(readOnly = true)
  public YksilonOsaaminenDto get(JodUser user, UUID id) {
    return repository
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapYksilonOsaaminen)
        .orElseThrow(() -> new NotFoundException("Not found"));
  }

  public void delete(JodUser user, Set<UUID> ids) {
    final var yksilo = yksiloRepository.getReferenceById(user.getId());
    if (repository.deleteByYksiloIdAndIdIn(yksilo.getId(), ids) != ids.size()) {
      throw new NotFoundException("Not found");
    }
    yksilo.updated();
    this.yksiloRepository.save(yksilo);
  }

  Set<Osaaminen> getOsaamiset(Set<URI> ids) {
    var osaamiset = osaamisetRepository.findByUriIn(ids);
    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown Osaaminen");
    }
    return osaamiset;
  }

  void addLahteenOsaamiset(OsaamisenLahde lahde, Set<Osaaminen> osaamiset) {
    updateLahteenOsaamiset(lahde, osaamiset, false);
  }

  void updateLahteenOsaamiset(OsaamisenLahde lahde, Set<Osaaminen> osaamiset) {
    updateLahteenOsaamiset(lahde, osaamiset, true);
  }

  void deleteAll(Set<YksilonOsaaminen> osaamiset) {
    repository.deleteAll(osaamiset);
  }

  /*
   * Update osaamiset for a given OsaamisenLahde. Filters duplicates and optinally removes Osaaminen
   * not present in the updated set.
   */
  private void updateLahteenOsaamiset(
      OsaamisenLahde lahde, Set<Osaaminen> updated, boolean removeOldOsaamiset) {
    var added = new HashSet<>(updated);
    var deleted = new ArrayList<YksilonOsaaminen>();

    for (var i = lahde.getOsaamiset().iterator(); i.hasNext(); ) {
      var o = i.next();
      if (added.contains(o.getOsaaminen())) {
        // filter out duplicates
        added.remove(o.getOsaaminen());
      } else if (removeOldOsaamiset) {
        i.remove();
        deleted.add(o);
      }
    }

    var count = repository.countByYksilo(lahde.getYksilo());
    if (count - deleted.size() + added.size() > Limits.OSAAMINEN) {
      throw new ProfileLimitException(ProfileItem.OSAAMINEN);
    }

    repository.deleteAll(deleted);
    lahde
        .getOsaamiset()
        .addAll(
            repository.saveAll(added.stream().map(o -> new YksilonOsaaminen(lahde, o)).toList()));
    lahde.getYksilo().updated();
    this.yksiloRepository.save(lahde.getYksilo());
  }
}
