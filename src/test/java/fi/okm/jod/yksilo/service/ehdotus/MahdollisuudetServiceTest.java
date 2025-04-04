/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.ehdotus;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

@Import(MahdollisuudetService.class)
class MahdollisuudetServiceTest extends AbstractServiceTest {

  private static final UUID koulutusIdActive =
      UUID.fromString("481e204a-691a-48dd-9b01-7f08d5858db9");
  private static final UUID koulutusIdInactive =
      UUID.fromString("c74eed41-c729-433e-8d36-4fc7527fe3df");
  private static final UUID tyoIdActive = UUID.fromString("ca466237-ce1d-4aca-9f9b-2ed566ef4f94");
  private static final UUID tyoIdInactive = UUID.fromString("af34f11f-05b5-434c-963a-df6d89a2149b");

  @Autowired private MahdollisuudetService mahdollisuudetService;

  @BeforeEach
  void setUp() {
    var em = entityManager.getEntityManager();

    em.createQuery("DELETE FROM Koulutusmahdollisuus").executeUpdate();
    em.createQuery("DELETE FROM Tyomahdollisuus").executeUpdate();

    createKoulutusMahdollisuus(em, koulutusIdActive, true, KoulutusmahdollisuusTyyppi.TUTKINTO);
    createKoulutusMahdollisuusKaannos(
        em, koulutusIdActive, Kieli.FI, "Koulutusmahdollisuus", "Kuvaus", "Tiivistelmä");
    createKoulutusMahdollisuusKaannos(
        em, koulutusIdActive, Kieli.EN, "Educational opportunity", "Description", "Summary");

    createKoulutusMahdollisuus(
        em, koulutusIdInactive, false, KoulutusmahdollisuusTyyppi.EI_TUTKINTO);
    createKoulutusMahdollisuusKaannos(
        em, koulutusIdInactive, Kieli.FI, "Koulutusmahdollisuus", "Kuvaus", "Tiivistelmä");
    createKoulutusMahdollisuusKaannos(
        em, koulutusIdInactive, Kieli.EN, "Educational opportunity", "Description", "Summary");

    createTyoMahdollisuus(em, tyoIdActive, true, TyomahdollisuusAineisto.TMT);
    createTyoMahdollisuusKaannos(
        em,
        tyoIdActive,
        Kieli.FI,
        "Työmahdollisuus",
        "Kuvaus",
        "Tiivistelmä",
        "Tehtävät",
        "Vaatimukset");
    createTyoMahdollisuusKaannos(
        em,
        tyoIdActive,
        Kieli.EN,
        "Job opportunity",
        "Description",
        "Summary",
        "Tasks",
        "Requirements");
    createTyoMahdollisuus(em, tyoIdInactive, false, TyomahdollisuusAineisto.AMMATTITIETO);
    createTyoMahdollisuusKaannos(
        em,
        tyoIdInactive,
        Kieli.FI,
        "Työmahdollisuus",
        "Kuvaus",
        "Tiivistelmä",
        "Tehtävät",
        "Vaatimukset");
    createTyoMahdollisuusKaannos(
        em,
        tyoIdInactive,
        Kieli.EN,
        "Job opportunity",
        "Description",
        "Summary",
        "Tasks",
        "Requirements");

    entityManager.flush();
  }

  private static void createKoulutusMahdollisuus(
      EntityManager em, UUID id, boolean active, KoulutusmahdollisuusTyyppi tyyppi) {
    em.createNativeQuery(
            "INSERT INTO koulutusmahdollisuus (id, aktiivinen, tyyppi) VALUES (?1, ?2, ?3)")
        .setParameter(1, id)
        .setParameter(2, active)
        .setParameter(3, tyyppi.name())
        .executeUpdate();
  }

  private static void createKoulutusMahdollisuusKaannos(
      EntityManager em,
      UUID id,
      Kieli language,
      String header,
      String description,
      String summary) {
    em.createNativeQuery(
            "INSERT INTO koulutusmahdollisuus_kaannos (koulutusmahdollisuus_id, kaannos_key, otsikko, kuvaus, tiivistelma) VALUES (?1, ?2, ?3, ?4, ?5)")
        .setParameter(1, id)
        .setParameter(2, language.name())
        .setParameter(3, header)
        .setParameter(4, description)
        .setParameter(5, summary)
        .executeUpdate();
  }

  private static void createTyoMahdollisuus(
      EntityManager em, UUID id, boolean active, TyomahdollisuusAineisto source) {
    em.createNativeQuery(
            "INSERT INTO tyomahdollisuus (id, aktiivinen, aineisto) VALUES (?1, ?2, ?3)")
        .setParameter(1, id)
        .setParameter(2, active)
        .setParameter(3, source.name())
        .executeUpdate();
  }

  private static void createTyoMahdollisuusKaannos(
      EntityManager em,
      UUID id,
      Kieli kieli,
      String jobOpportunity,
      String description,
      String summary,
      String tasks,
      String requirements) {
    em.createNativeQuery(
            "INSERT INTO tyomahdollisuus_kaannos (tyomahdollisuus_id, kaannos_key, otsikko, kuvaus, tiivistelma, tehtavat, yleiset_vaatimukset) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)")
        .setParameter(1, id)
        .setParameter(2, kieli.name())
        .setParameter(3, jobOpportunity)
        .setParameter(4, description)
        .setParameter(5, summary)
        .setParameter(6, tasks)
        .setParameter(7, requirements)
        .executeUpdate();
  }

  @SuppressWarnings("java:S2699")
  @Test
  void shouldClearCacheAtStartup() {
    mahdollisuudetService.clearCacheAtStartup();
    // Note: This just verifies the method executes, as it's just clearing the cache
  }

  @Test
  void shouldFetchTyoAndKoulutusMahdollisuusIdsWithTypes() {
    var result =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI);

    assertThat(result)
        .as("Only actives should be included.")
        .hasSize(2)
        .containsEntry(koulutusIdActive, MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
        .containsEntry(tyoIdActive, MahdollisuusTyyppi.TYOMAHDOLLISUUS);
  }

  @Test
  void shouldFetchTyoAndKoulutusMahdollisuusIdsWithTypes_EmptyResults() {
    var em = entityManager.getEntityManager();
    em.createNativeQuery("DELETE FROM koulutusmahdollisuus_kaannos WHERE kaannos_key = 'EN'")
        .executeUpdate();
    em.createNativeQuery("DELETE FROM tyomahdollisuus_kaannos WHERE kaannos_key = 'EN'")
        .executeUpdate();
    entityManager.flush();

    var result =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.EN);

    assertThat(result).isEmpty();
  }
}
