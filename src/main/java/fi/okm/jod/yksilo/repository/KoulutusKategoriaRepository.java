/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.repository;

import fi.okm.jod.yksilo.entity.KoulutusKategoria;
import fi.okm.jod.yksilo.entity.Yksilo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface KoulutusKategoriaRepository extends JpaRepository<KoulutusKategoria, UUID> {

  List<KoulutusKategoria> findAllByYksilo(Yksilo yksilo);

  @Query(
      """
      DELETE FROM KoulutusKategoria k
      WHERE k.yksilo = :yksilo
      AND NOT EXISTS
      (SELECT 1 FROM Koulutus o WHERE o.kategoria = k)
      """)
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  void deleteOrphaned(Yksilo yksilo);
}
