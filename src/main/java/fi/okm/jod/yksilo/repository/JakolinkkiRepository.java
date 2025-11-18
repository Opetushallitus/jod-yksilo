/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.Jakolinkki;
import fi.okm.jod.yksilo.repository.projection.JakolinkkiDetails;
import fi.okm.jod.yksilo.repository.projection.JakolinkkiSettings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JakolinkkiRepository extends JpaRepository<Jakolinkki, UUID> {
  @Query(
      value =
          "SELECT tunnistus.create_jakolinkki(:henkiloId, :voimassaAsti, :nimiJaettu, :emailJaettu)",
      nativeQuery = true)
  UUID createJakolinkki(
      String henkiloId, Instant voimassaAsti, boolean nimiJaettu, boolean emailJaettu);

  @Query(
      value =
          "SELECT tunnistus.update_jakolinkki(:henkiloId, :jakolinkkiId, :voimassaAsti, :nimiJaettu, :emailJaettu)",
      nativeQuery = true)
  void updateJakolinkki(
      String henkiloId,
      UUID jakolinkkiId,
      Instant voimassaAsti,
      boolean nimiJaettu,
      boolean emailJaettu);

  @Query(
      value =
          "SELECT jakolinkki_id, ulkoinen_id, voimassa_asti, nimi_jaettu, email_jaettu FROM tunnistus.get_jakolinkki(:henkiloId, :jakolinkkiId)",
      nativeQuery = true)
  Optional<JakolinkkiSettings> getJakolinkki(String henkiloId, UUID jakolinkkiId);

  @Query(
      value =
          "SELECT jakolinkki_id, ulkoinen_id, voimassa_asti, nimi_jaettu, email_jaettu FROM tunnistus.get_jakolinkit(:henkiloId)",
      nativeQuery = true)
  List<JakolinkkiSettings> getJakolinkit(String henkiloId);

  @Query(
      value =
          "SELECT jakolinkki_id, email, etunimi, sukunimi, voimassa_asti, nimi_jaettu, email_jaettu FROM tunnistus.get_jakolinkki_by_ulkoinen_id(:ulkoinenId)",
      nativeQuery = true)
  Optional<JakolinkkiDetails> getJakolinkkiByUlkoinenId(UUID ulkoinenId);

  @Query(
      value = "SELECT tunnistus.delete_jakolinkki(:henkiloId, :jakolinkkiId)",
      nativeQuery = true)
  void deleteJakolinkki(String henkiloId, UUID jakolinkkiId);
}
