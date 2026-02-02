/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KoulutusmahdollisuusRepository extends JpaRepository<Koulutusmahdollisuus, UUID> {
  Page<Koulutusmahdollisuus> findByIdIn(Set<UUID> ids, Pageable pageable);

  @Query(
      value =
          """
    SELECT * FROM mahdollisuus_view mv
    WHERE mv.id IN (
      SELECT k.koulutusmahdollisuus_id FROM koulutusmahdollisuus_kaannos k
      WHERE k.kaannos_key = :lang
        AND (
          k.otsikko ILIKE CONCAT('%', :q, '%')
          OR k.kuvaus ILIKE CONCAT('%', :q, '%')
          OR k.tiivistelma ILIKE CONCAT('%', :q, '%')
        )
      UNION
      SELECT t.tyomahdollisuus_id FROM tyomahdollisuus_kaannos t
      WHERE t.kaannos_key = :lang
        AND (
          t.otsikko ILIKE CONCAT('%', :q, '%')
          OR t.kuvaus ILIKE CONCAT('%', :q, '%')
          OR t.tiivistelma ILIKE CONCAT('%', :q, '%')
        )
    )
    """,
      nativeQuery = true)
  List<Koulutusmahdollisuus> search(@Param("lang") String lang, @Param("q") String query);
}
