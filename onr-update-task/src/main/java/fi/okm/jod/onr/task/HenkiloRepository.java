/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for reading and updating tunnistus.henkilo rows. */
@Repository
class HenkiloRepository {

  private static final Logger log = LoggerFactory.getLogger(HenkiloRepository.class);

  private final JdbcClient jdbc;

  HenkiloRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Fetches a batch of henkilo rows with FIN id but no oppijanumero and non-null names, ordered by
   * yksilo_id. Uses cursor-based pagination: pass {@code null} for the first batch, then the last
   * yksilo_id of the previous batch to advance.
   */
  @Transactional(readOnly = true)
  List<HenkiloRow> findBatchWithoutOppijanumero(int limit, UUID afterYksiloId) {
    return jdbc.sql(
            """
            SELECT yksilo_id, henkilo_id, etunimi, sukunimi
            FROM tunnistus.henkilo
            WHERE henkilo_id LIKE 'FIN:%'
              AND oppijanumero IS NULL
              AND etunimi IS NOT NULL
              AND sukunimi IS NOT NULL
              AND (:afterYksiloId::uuid IS NULL OR yksilo_id > :afterYksiloId)
            ORDER BY yksilo_id
            LIMIT :limit
            """)
        .param("limit", limit)
        .param("afterYksiloId", afterYksiloId)
        .query(
            (rs, _) ->
                new HenkiloRow(
                    rs.getObject("yksilo_id", UUID.class),
                    rs.getString("henkilo_id"),
                    rs.getString("etunimi"),
                    rs.getString("sukunimi")))
        .list();
  }

  /** Updates a single henkilo row with the oppijanumero. */
  @Transactional
  void updateOppijanumero(UUID yksiloId, String oppijanumero) {
    int updated =
        jdbc.sql(
                """
                UPDATE tunnistus.henkilo
                SET oppijanumero = :oppijanumero, muokattu = CURRENT_TIMESTAMP
                WHERE yksilo_id = :yksiloId AND oppijanumero IS NULL
                """)
            .param("oppijanumero", oppijanumero)
            .param("yksiloId", yksiloId)
            .update();
    if (updated != 1) {
      log.warn("Expected to update 1 row for yksilo_id={}, but updated {}", yksiloId, updated);
    }
  }
}
