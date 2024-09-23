/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface YksiloRepository extends JpaRepository<Yksilo, UUID> {

  @Query(value = "SELECT tunnistus.generate_yksilo_id(:henkiloId)", nativeQuery = true)
  UUID findIdByHenkiloId(String henkiloId);

  @Query(value = "SELECT tunnistus.remove_yksilo_id(:yksiloId)", nativeQuery = true)
  void removeId(UUID yksiloId);

  @Query(value = "SELECT k.uri FROM Yksilo y JOIN y.osaamisKiinnostukset k WHERE y = :yksilo")
  Set<String> findOsaamisKiinnostukset(Yksilo yksilo);
}
