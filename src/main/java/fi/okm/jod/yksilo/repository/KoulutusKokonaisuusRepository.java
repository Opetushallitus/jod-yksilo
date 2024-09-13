/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KoulutusKokonaisuusRepository extends JpaRepository<KoulutusKokonaisuus, UUID> {

  List<KoulutusKokonaisuus> findByYksiloId(UUID yksiloId);

  Optional<KoulutusKokonaisuus> findByYksiloIdAndId(UUID yksiloId, UUID id);

  long countByYksilo(Yksilo yksilo);
}