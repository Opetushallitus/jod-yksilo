/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.entity.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.projection.TyomahdollisuusMetadata;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TyomahdollisuusRepository extends JpaRepository<Tyomahdollisuus, UUID> {

  @Query(
      "SELECT t FROM Tyomahdollisuus t JOIN t.kaannos k WHERE k.otsikko IN :name AND KEY(k) = :kieli")
  List<Tyomahdollisuus> findByOtsikkoIn(Iterable<String> name, Kieli kieli);

  @Query(
      "SELECT new fi.okm.jod.yksilo.entity.projection.TyomahdollisuusMetadata(t.id, t.mahdollisuusId) FROM Tyomahdollisuus t")
  List<TyomahdollisuusMetadata> fetchAllTyomahdollisuusMetadata();
}
