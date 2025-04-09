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
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KoulutusRepository
    extends JpaRepository<Koulutus, UUID>, OsaamisenLahdeRepository<Koulutus> {

  long countByKokonaisuus(KoulutusKokonaisuus kokonaisuus);

  Optional<Koulutus> findByKokonaisuusYksiloIdAndId(UUID yksiloId, UUID id);

  List<Koulutus> findByKokonaisuusYksiloIdAndKokonaisuusId(UUID yksiloId, UUID kokonaisuusId);

  @Override
  default Optional<Koulutus> findBy(JodUser user, OsaamisenLahdeDto lahde) {
    return lahde.tyyppi() == KOULUTUS && lahde.id().isPresent()
        ? lahde.id().flatMap(lahdeId -> findByKokonaisuusYksiloIdAndId(user.getId(), lahdeId))
        : Optional.empty();
  }

  Optional<Koulutus> findByKokonaisuusYksiloIdAndKokonaisuusIdAndId(
      UUID id, UUID tyopaikkaId, UUID id1);

  List<Koulutus> findByKokonaisuusYksiloIdAndIdInAndOsaamisenTunnistusStatusIn(
      UUID id, List<UUID> uuids, Set<OsaamisenTunnistusStatus> statusesToReturn);
}
