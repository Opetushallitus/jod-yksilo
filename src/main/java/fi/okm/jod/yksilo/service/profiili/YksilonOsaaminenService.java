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
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen_;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.OsaamisenLahdeRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final OsaaminenRepository osaamisetRepository;
  private final List<OsaamisenLahdeRepository<?>> lahteet;

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

  public YksilonOsaaminenDto get(JodUser user, UUID id) {
    return repository
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapYksilonOsaaminen)
        .orElseThrow(() -> new NotFoundException("Not found"));
  }

  public void delete(JodUser user, Set<UUID> ids) {
    if (repository.deleteByYksiloIdAndIdIn(user.getId(), ids) != ids.size()) {
      throw new NotFoundException("Not found");
    }
  }

  List<YksilonOsaaminen> add(OsaamisenLahde lahde, Set<URI> ids) {
    var osaamiset = osaamisetRepository.findByUriIn(ids.stream().map(Object::toString).toList());
    if (osaamiset.size() != ids.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    }
    var entities =
        repository.saveAll(osaamiset.stream().map(o -> new YksilonOsaaminen(lahde, o)).toList());
    lahde.getOsaamiset().addAll(entities);
    return entities;
  }

  void update(OsaamisenLahde lahde, Set<URI> ids) {

    var deleted = new ArrayList<YksilonOsaaminen>();
    var refs = ids.stream().map(Object::toString).collect(Collectors.toSet());

    for (var i = lahde.getOsaamiset().iterator(); i.hasNext(); ) {
      var o = i.next();
      final String uri = o.getOsaaminen().getUri();
      if (!refs.contains(uri)) {
        i.remove();
        deleted.add(o);
      } else {
        refs.remove(uri);
      }
    }
    repository.deleteAllInBatch(deleted);

    var osaamiset = osaamisetRepository.findByUriIn(refs);
    if (osaamiset.size() != refs.size()) {
      throw new ServiceValidationException("Unknown osaaminen");
    }

    lahde
        .getOsaamiset()
        .addAll(
            repository.saveAll(
                osaamiset.stream().map(o -> new YksilonOsaaminen(lahde, o)).toList()));
  }

  void deleteAll(Set<YksilonOsaaminen> osaamiset) {
    // Note. bypasses persistence context
    repository.deleteAllInBatch(osaamiset);
  }
}
