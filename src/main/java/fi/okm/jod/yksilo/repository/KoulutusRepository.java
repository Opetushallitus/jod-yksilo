/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import static fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi.KOULUTUS;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.OsaamisenLahdeDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKategoria;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface KoulutusRepository
    extends JpaRepository<Koulutus, UUID>, OsaamisenLahdeRepository<Koulutus> {

  Optional<Koulutus> findByYksiloIdAndId(UUID yksiloId, UUID id);

  List<Koulutus> findByYksiloAndIdIn(Yksilo yksilo, Collection<UUID> id);

  @EntityGraph(attributePaths = {"kategoria", "kaannos"})
  List<Koulutus> findByYksilo(Yksilo yksilo, Sort sort);

  @EntityGraph(attributePaths = {"kategoria", "kaannos"})
  List<Koulutus> findByYksiloAndKategoriaId(Yksilo yksilo, @Nullable UUID kategoriaId, Sort sort);

  @Override
  default Optional<Koulutus> findBy(JodUser user, OsaamisenLahdeDto lahde) {
    return lahde.tyyppi() == KOULUTUS
        ? findByYksiloIdAndId(user.getId(), lahde.id())
        : Optional.empty();
  }

  @Modifying
  @Query("DELETE FROM YksilonOsaaminen o WHERE o.koulutus.id in :koulutusIds")
  void deleteOsaamiset(Collection<UUID> koulutusIds);

  @Query(
      "SELECT k FROM Koulutus k WHERE ((:kategoria IS NULL AND k.kategoria IS NULL) OR k.kategoria = :kategoria) AND k.id NOT IN :ids")
  List<Koulutus> findByKategoriaAndIdNotIn(
      @Nullable KoulutusKategoria kategoria, Collection<UUID> ids);

  int countByYksilo(Yksilo yksilo);
}
