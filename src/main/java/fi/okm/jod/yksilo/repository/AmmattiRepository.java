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
import fi.okm.jod.yksilo.dto.AmmattiDto;
import fi.okm.jod.yksilo.entity.Ammatti;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface AmmattiRepository extends Repository<Ammatti, Long> {
  Logger log = LoggerFactory.getLogger(AmmattiRepository.class);

  @Query("SELECT v.versio FROM AmmattiVersio v WHERE v.id = 1")
  long currentVersion();

  Page<Ammatti> findAll(Pageable page);

  Stream<Ammatti> findAll(Sort sort);

  List<Ammatti> findByUriIn(Collection<String> uri);

  @Transactional(readOnly = true)
  default Versioned<Map<URI, AmmattiDto>> refreshAll(
      @Nullable Versioned<Map<URI, AmmattiDto>> previous) {
    long version = currentVersion();
    if (previous == null || previous.version() != version) {
      final var map =
          findAll(Sort.by("koodi", "id"))
              .map(
                  it ->
                      new AmmattiDto(
                          URI.create(it.getUri()), it.getKoodi(), it.getNimi(), it.getKuvaus()))
              .collect(
                  Collectors.toMap(
                      AmmattiDto::uri,
                      Function.identity(),
                      (existing, replacement) -> existing,
                      LinkedHashMap::new));
      return new Versioned<>(version, Collections.unmodifiableSequencedMap(map));
    }
    return previous;
  }
}
