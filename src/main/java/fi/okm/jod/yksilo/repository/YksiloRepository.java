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
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface YksiloRepository extends JpaRepository<Yksilo, UUID> {

  @Query(value = "SELECT tunnistus.generate_yksilo_id(:henkiloId)", nativeQuery = true)
  UUID findIdByHenkiloId(String henkiloId);

  @Query(value = "SELECT tunnistus.remove_yksilo_id(:yksiloId)", nativeQuery = true)
  void removeId(UUID yksiloId);

  @Query(value = "SELECT tunnistus.update_yksilo_email(:henkiloId, :email)", nativeQuery = true)
  void updateEmail(String henkiloId, @Nullable String email);

  @Query(
      value = "SELECT tunnistus.update_yksilo_name(:henkiloId, :etunimi, :sukunimi)",
      nativeQuery = true)
  void updateName(String henkiloId, String etunimi, String sukunimi);

  @Query(value = "SELECT tunnistus.read_yksilo_email(:henkiloId)", nativeQuery = true)
  Optional<String> getEmail(String henkiloId);

  @Query(value = "SELECT k.uri FROM Yksilo y JOIN y.osaamisKiinnostukset k WHERE y = :yksilo")
  Set<String> findOsaamisKiinnostukset(Yksilo yksilo);

  @Query(value = "SELECT k.uri FROM Yksilo y JOIN y.ammattiKiinnostukset k WHERE y = :yksilo")
  Set<String> findAmmattiKiinnostukset(Yksilo yksilo);

  @Query(
      value =
          "SELECT y FROM Yksilo y WHERE y.muokattu > :muokattuJalkeen ORDER BY y.muokattu ASC, y.id")
  Page<Yksilo> findAllModifiedAfter(Instant muokattuJalkeen, Pageable pageable);
}
