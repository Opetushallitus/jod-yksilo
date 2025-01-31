/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.Versioned;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Osaaminen_;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface OsaaminenRepository extends Repository<Osaaminen, Long> {

  Stream<Osaaminen> findAll(Sort sort);

  Page<Osaaminen> findAll(Pageable page);

  List<Osaaminen> findByUriIn(Collection<String> uri);

  @Transactional(readOnly = true)
  default SequencedMap<URI, OsaaminenDto> loadAll() {
    final var map =
        findAll(Sort.by("id"))
            .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus()))
            .collect(
                Collectors.toMap(
                    OsaaminenDto::uri,
                    Function.identity(),
                    (existing, replacement) -> existing,
                    LinkedHashMap::new));
    return Collections.unmodifiableSequencedMap(map);
  }

  @Query("SELECT v.versio FROM OsaaminenVersio v WHERE v.id = 1")
  long currentVersion();

  @Transactional(readOnly = true)
  default Versioned<Map<URI, OsaaminenDto>> refreshAll(
      @Nullable Versioned<Map<URI, OsaaminenDto>> previous) {
    long version = currentVersion();
    if (previous == null || previous.version() != version) {
      final var map =
          findAll(Sort.by(Osaaminen_.URI, Osaaminen_.ID))
              .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus()))
              .collect(
                  Collectors.toMap(
                      OsaaminenDto::uri,
                      Function.identity(),
                      (existing, replacement) -> existing,
                      LinkedHashMap::new));
      return new Versioned<>(version, Collections.unmodifiableSequencedMap(map));
    }
    return previous;
  }
}
