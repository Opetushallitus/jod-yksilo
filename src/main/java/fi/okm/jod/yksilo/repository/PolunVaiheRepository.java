/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.PolunVaihe;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolunVaiheRepository extends JpaRepository<PolunVaihe, UUID> {
  Optional<PolunVaihe>
      findByPolunSuunnitelmaPaamaaraYksiloIdAndPolunSuunnitelmaPaamaaraIdAndPolunSuunnitelmaIdAndId(
          UUID yksiloId, UUID paamaaraId, UUID polunSuunnitelmaId, UUID id);

  long countByPolunSuunnitelma(PolunSuunnitelma polunSuunnitelma);
}
