/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Yksilo;
import jakarta.persistence.PersistenceException;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The database accepts only canonical Koski identifiers, as they are compared as a whole. */
class KoskiTunnisteConstraintTest extends AbstractServiceTest {

  @ParameterizedTest
  @CsvSource(
      value = {
        // oid, lahde, lahde_id, toimija_oid
        "1.2.246.562.15.20709430670, NULL, NULL, NULL",
        "NULL, NULL, 60232459, NULL",
        "NULL, virta, 60232459, 1.2.246.562.10.38515028629"
      },
      nullValues = "NULL")
  void shouldAcceptCanonicalIdentifier(
      String oid, String lahde, String lahdeId, String toimijaOid) {
    assertThat(setKoskiTunniste(oid, lahde, lahdeId, toimijaOid)).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        // the oid identifies alone, other fields would make equal identifiers differ
        "1.2.246.562.15.20709430670, NULL, NULL, 1.2.246.562.10.38515028629",
        "1.2.246.562.15.20709430670, virta, NULL, NULL",
        "1.2.246.562.15.20709430670, NULL, 60232459, NULL",
        // no identifier is stored as no row
        "NULL, NULL, NULL, NULL",
        // the id in the source system is required when there is no oid
        "NULL, virta, NULL, NULL",
        "NULL, NULL, NULL, 1.2.246.562.10.38515028629",
      },
      nullValues = "NULL")
  void shouldRejectNonCanonicalIdentifier(
      String oid, String lahde, String lahdeId, String toimijaOid) {
    assertThatThrownBy(() -> setKoskiTunniste(oid, lahde, lahdeId, toimijaOid))
        .isInstanceOf(PersistenceException.class);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "'', NULL, NULL, NULL",
        "NULL, '', 60232459, NULL",
        "NULL, NULL, '', NULL",
        "NULL, NULL, 60232459, ''"
      },
      nullValues = "NULL")
  void shouldRejectEmptyValues(String oid, String lahde, String lahdeId, String toimijaOid) {
    assertThatThrownBy(() -> setKoskiTunniste(oid, lahde, lahdeId, toimijaOid))
        .isInstanceOf(PersistenceException.class);
  }

  private int setKoskiTunniste(String oid, String lahde, String lahdeId, String toimijaOid) {
    var id = newKoulutusKokonaisuus();
    return entityManager
        .getEntityManager()
        .createNativeQuery(
            "insert into koulutus_kokonaisuus_koski_tunniste (koulutus_kokonaisuus_id, oid,"
                + " lahdejarjestelma, lahdejarjestelma_id, toimija_oid)"
                + " values (:id, :oid, :lahde, :lahdeId, :toimijaOid)")
        .setParameter("oid", oid)
        .setParameter("lahde", lahde)
        .setParameter("lahdeId", lahdeId)
        .setParameter("toimijaOid", toimijaOid)
        .setParameter("id", id)
        .executeUpdate();
  }

  private UUID newKoulutusKokonaisuus() {
    var yksilo = entityManager.find(Yksilo.class, user.getId());
    return entityManager.persistAndFlush(new KoulutusKokonaisuus(yksilo, ls("Koulu"))).getId();
  }
}
