/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository.koodisto;

import fi.okm.jod.yksilo.entity.koodisto.Koulutuskoodi;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface KoulutuskoodiRepository extends Repository<Koulutuskoodi, Long> {
  @EntityGraph(attributePaths = {"kaannos"})
  Optional<Koulutuskoodi> findByKoodi(String koodi);
}
