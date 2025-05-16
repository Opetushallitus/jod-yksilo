/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.entity.Jakauma;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.KoulutusmahdollisuusJakauma;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.TyomahdollisuusJakauma;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql(
    scripts = {"/data/mock-tunnistus.sql", "/schema.sql", "/data.sql"},
    config = @SqlConfig(separator = ";;;"),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(
    scripts = {"/data/mahdollisuudet.sql"},
    config = @SqlConfig(separator = ";;;"),
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DirtiesContext
@Import({
  TyomahdollisuusService.class,
  KoulutusmahdollisuusService.class,
  MahdollisuudetService.class
})
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("SLOW")
class MahdollisuusImportTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgreSQLContainer = TestUtil.createPostgresSQLContainer();

  private static final String KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE =
      "koulutusmahdollisuus_data.import";
  private static final String KOULUTUSMAHDOLLISUUS_UPDATE_WITH_EXTENDED_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-koulutus-update-import-data-extended.sql";
  private static final String KOULUTUSMAHDOLLISUUS_UPDATE_WITH_REDUCED_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-koulutus-update-import-data-reduced.sql";
  private static final String KOULUTUSMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-koulutus-delete-import-data.sql";

  private static final UUID KOULUTUSMAHDOLLISUUS_ID =
      UUID.fromString("30080e88-f292-48a3-9835-41950817abd3");
  private static final String KOULUTUSMAHDOLLISUUS_JAKAUMA_OSAAMINEN_KEY =
      "http://data.europa.eu/esco/skill/bfe4f330-d595-48c7-ab3c-f309471d6953";

  private static final String TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE = "tyomahdollisuus_data.import";
  private static final String TYOMAHDOLLISUUS_UPDATE_WITH_EXTENDED_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-tyo-update-import-data-extended.sql";
  private static final String TYOMAHDOLLISUUS_UPDATE_WITH_REDUCED_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-tyo-update-import-data-reduced.sql";
  private static final String TYOMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL =
      "/data/mahdollisuudet-tyo-delete-import-data.sql";

  private static final UUID TYOMAHDOLLISUUS_ID =
      UUID.fromString("bc77f514-8573-11ef-8f0c-b767f527df04");
  private static final String TYOMAHDOLLISUUS_JAKAUMA_TYON_JATKUVUUS_KEY = "PERMANENT";

  @Autowired private TestEntityManager entityManager;
  @Autowired private MahdollisuudetService mahdollisuudetService;

  @Test
  void shouldImportKoulutusMahdollisuusData_extendedDataset() throws IOException {
    // Initial state check
    var entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertNull(entity, "Koulutusmahdollisuus should not exist initially");

    // First import
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertKoulutusMahdollisuusFirstImport(entity);

    // Apply update script and re-import that does upsert
    runSqlScript(KOULUTUSMAHDOLLISUUS_UPDATE_WITH_EXTENDED_IMPORT_DATA_SQL);
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertNotNull(entity, "Koulutusmahdollisuus should exist after update");
    assertTrue(entity.isAktiivinen());
    assertEquals(KoulutusmahdollisuusTyyppi.TUTKINTO, entity.getTyyppi());
    // Check updated data
    var kaannos = entity.getKaannos();
    assertEquals(3, kaannos.size());
    assertOtsikko(
        kaannos,
        "Uusi Psykologian yliopistotutkinto",
        "Nya Kandidatexamen i psykologi",
        "New Degree in psychology");
    assertEquals(3, entity.getKoulutukset().size(), "1 more koulutus was added.");
    assertKesto(entity.getKesto(), 6, 51, 121);
    var jakaumat = entity.getJakaumat();
    assertEquals(6, jakaumat.size());
    assertJakauma(
        jakaumat.get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN),
        90,
        4,
        KOULUTUSMAHDOLLISUUS_JAKAUMA_OSAAMINEN_KEY,
        60);
    var newKey = "http://data.europa.eu/esco/skill/00920055-eb50-4cb8-a23d-3deedbf3753e";
    assertJakauma(jakaumat.get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN), 90, 4, newKey, 1);

    // Delete source of import data and re-import
    runSqlScript(KOULUTUSMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL);
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertFalse(entity.isAktiivinen());
  }

  private static void assertKoulutusMahdollisuusFirstImport(Koulutusmahdollisuus entity) {
    assertNotNull(entity, "Koulutusmahdollisuus should exist after import");
    assertTrue(entity.isAktiivinen());
    assertEquals(KoulutusmahdollisuusTyyppi.TUTKINTO, entity.getTyyppi());
    var kaannos = entity.getKaannos();
    assertOtsikko(
        kaannos,
        "Psykologian yliopistotutkinto",
        "Kandidatexamen i psykologi",
        "Degree in psychology");
    assertEquals(2, entity.getKoulutukset().size());
    var koulutukset = entity.getKoulutukset();
    var koulutusViite = koulutukset.iterator().next();
    assertEquals(3, koulutusViite.getKaannos().size());

    assertKesto(entity.getKesto(), 5, 50, 120);
    var jakaumat = entity.getJakaumat();
    assertEquals(6, jakaumat.size());
    assertJakauma(
        jakaumat.get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN),
        100,
        5,
        KOULUTUSMAHDOLLISUUS_JAKAUMA_OSAAMINEN_KEY,
        50);
  }

  private static void assertKesto(
      Koulutusmahdollisuus.KestoJakauma kesto, double minimi, double mediaani, double maksimi) {
    assertEquals(minimi, kesto.minimi());
    assertEquals(mediaani, kesto.mediaani());
    assertEquals(maksimi, kesto.maksimi());
  }

  private static Optional<Jakauma.Arvo> assertJakauma(
      KoulutusmahdollisuusJakauma jakauma, int maara, int tyhjia, String arvo, double osuus) {
    assertEquals(maara, jakauma.getMaara());
    assertEquals(tyhjia, jakauma.getTyhjia());
    var foundArvo = jakauma.getArvot().stream().filter(a -> arvo.equals(a.arvo())).findFirst();
    if (foundArvo.isPresent()) {
      var arvo1 = foundArvo.get();
      assertEquals(osuus, arvo1.osuus());
    }
    return foundArvo;
  }

  private static void assertOtsikko(
      Map<Kieli, Koulutusmahdollisuus.Kaannos> kaannos,
      String expectedFi,
      String expectedSv,
      String expectedEn) {
    assertEquals(expectedFi, kaannos.get(Kieli.FI).otsikko());
    assertEquals(expectedSv, kaannos.get(Kieli.SV).otsikko());
    assertEquals(expectedEn, kaannos.get(Kieli.EN).otsikko());
  }

  private void runSqlProcedure(String procedureName) {
    entityManager.getEntityManager().createStoredProcedureQuery(procedureName).execute();
    entityManager.flush();
    entityManager.clear();
  }

  private void runSqlScript(String scriptPath) throws IOException {
    String sqlScript;
    try (var resourceAsStream = getClass().getResourceAsStream(scriptPath)) {
      assert resourceAsStream != null;
      var bytes = resourceAsStream.readAllBytes();
      sqlScript = new String(bytes);
    }
    entityManager.getEntityManager().createNativeQuery(sqlScript).executeUpdate();
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void shouldImportKoulutusMahdollisuusData_reducedDataset() throws IOException {
    // Initial state check
    var entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertNull(entity, "Koulutusmahdollisuus should not exist initially");

    // First import
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertKoulutusMahdollisuusFirstImport(entity);
    assertNotNull(entity.getJakaumat().get(KoulutusmahdollisuusJakaumaTyyppi.KOULUTUSALA));

    // Apply update script and re-import that does upsert
    runSqlScript(KOULUTUSMAHDOLLISUUS_UPDATE_WITH_REDUCED_IMPORT_DATA_SQL);
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertNotNull(entity, "Koulutusmahdollisuus should exist after update");
    assertTrue(entity.isAktiivinen());
    assertEquals(KoulutusmahdollisuusTyyppi.TUTKINTO, entity.getTyyppi());
    // Check updated data
    var kaannos = entity.getKaannos();
    assertEquals(3, kaannos.size());
    assertOtsikko(
        kaannos,
        "Uusi Psykologian yliopistotutkinto",
        "Nya Kandidatexamen i psykologi",
        "New Degree in psychology");
    assertEquals(
        1, entity.getKoulutukset().size(), "1 koulutus was removed, so it should be less now.");
    var koulutukset = entity.getKoulutukset();
    var koulutusViite = koulutukset.iterator().next();
    assertEquals(2, koulutusViite.getKaannos().size(), "SV was removed, so it should be less now.");
    assertKesto(entity.getKesto(), 6, 51, 121);
    var jakaumat = entity.getJakaumat();
    assertEquals(
        5, jakaumat.size(), "1 koulutusalaJakauma was removed, so it should be 1 less now.");
    assertNull(jakaumat.get(KoulutusmahdollisuusJakaumaTyyppi.KOULUTUSALA));
    assertFalse(
        assertJakauma(
                jakaumat.get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN),
                90,
                4,
                KOULUTUSMAHDOLLISUUS_JAKAUMA_OSAAMINEN_KEY,
                0)
            .isPresent(),
        "Key was removed, so it shouldn't be present");

    // Delete source of import data and re-import
    runSqlScript(KOULUTUSMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL);
    runSqlProcedure(KOULUTUSMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Koulutusmahdollisuus.class, KOULUTUSMAHDOLLISUUS_ID);
    assertFalse(entity.isAktiivinen());
  }

  @Test
  void shouldImportTyoMahdollisuusData_extendedDataset() throws IOException {
    // Initial state check
    var entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertNull(entity, "Tyomahdollisuus should not exist initially");

    // First import
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertTyomahdollisuusFirstImport(entity);

    // Apply update script and re-import that does upsert
    runSqlScript(TYOMAHDOLLISUUS_UPDATE_WITH_EXTENDED_IMPORT_DATA_SQL);
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertNotNull(entity, "Should exist after import.");
    assertEquals(TyomahdollisuusAineisto.AMMATTITIETO, entity.getAineisto());
    assertTrue(entity.isAktiivinen());
    // Check updated data
    assertEquals(URI.create("http://data.europa.eu/esco/isco/C2149"), entity.getAmmattiryhma());
    var kaannos = entity.getKaannos();
    assertEquals(3, kaannos.size());
    assertTyoOtsikko(kaannos, "Uusi Kouluttaja", "Nya Kouluttaja", "New Kouluttaja");
    var jakaumat = entity.getJakaumat();
    assertEquals(17, jakaumat.size());
    assertJakauma(
        jakaumat.get(TyomahdollisuusJakaumaTyyppi.TYON_JATKUVUUS),
        30,
        1,
        TYOMAHDOLLISUUS_JAKAUMA_TYON_JATKUVUUS_KEY,
        80.5);

    // Delete source of import data and re-import
    runSqlScript(TYOMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL);
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertFalse(entity.isAktiivinen());
  }

  private static void assertTyomahdollisuusFirstImport(Tyomahdollisuus entity) {
    assertNotNull(entity, "Tyomahdollisuus should exist after import");
    assertEquals(TyomahdollisuusAineisto.TMT, entity.getAineisto());
    assertTrue(entity.isAktiivinen());
    assertEquals(URI.create(""), entity.getAmmattiryhma());
    var kaannos = entity.getKaannos();
    assertEquals(3, kaannos.size());
    assertTyoOtsikko(kaannos, "Kouluttaja", "Kouluttaja", "Kouluttaja");
    var jakaumat = entity.getJakaumat();
    assertEquals(17, jakaumat.size());
    assertJakauma(
        jakaumat.get(TyomahdollisuusJakaumaTyyppi.TYON_JATKUVUUS),
        24,
        0,
        TYOMAHDOLLISUUS_JAKAUMA_TYON_JATKUVUUS_KEY,
        66.66666666666666);
  }

  private static void assertTyoOtsikko(
      Map<Kieli, Tyomahdollisuus.Kaannos> kaannos,
      String expectedFi,
      String expectedSv,
      String expectedEn) {
    assertEquals(expectedFi, kaannos.get(Kieli.FI).otsikko());
    var sv = kaannos.get(Kieli.SV);
    if (expectedSv != null) {
      assertEquals(expectedSv, sv.otsikko());
    } else {
      assertNull(sv);
    }
    assertEquals(expectedEn, kaannos.get(Kieli.EN).otsikko());
  }

  private static Optional<Jakauma.Arvo> assertJakauma(
      TyomahdollisuusJakauma jakauma, int maara, int tyhjia, String arvo, double osuus) {
    assertEquals(maara, jakauma.getMaara());
    assertEquals(tyhjia, jakauma.getTyhjia());
    var foundArvo = jakauma.getArvot().stream().filter(a -> arvo.equals(a.arvo())).findFirst();
    if (foundArvo.isPresent()) {
      var arvo1 = foundArvo.get();
      assertEquals(osuus, arvo1.osuus());
    }
    return foundArvo;
  }

  @Test
  void shouldImportTyoMahdollisuusData_reducedDataset() throws IOException {
    // Initial state check
    var entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertNull(entity, "Tyomahdollisuus should not exist initially");

    // First import
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertTyomahdollisuusFirstImport(entity);

    // Apply update script and re-import that does upsert
    runSqlScript(TYOMAHDOLLISUUS_UPDATE_WITH_REDUCED_IMPORT_DATA_SQL);
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertNotNull(entity, "Should exist after import.");
    assertEquals(TyomahdollisuusAineisto.AMMATTITIETO, entity.getAineisto());
    assertTrue(entity.isAktiivinen());
    // Check updated data
    assertEquals(URI.create("http://data.europa.eu/esco/isco/C2149"), entity.getAmmattiryhma());
    var kaannos = entity.getKaannos();
    assertEquals(2, kaannos.size());
    assertTyoOtsikko(kaannos, "Uusi Kouluttaja", null, "New Kouluttaja");
    var jakaumat = entity.getJakaumat();
    assertEquals(16, jakaumat.size(), "ajokorttiJakauma was removed, so it should be 1 less now.");
    assertFalse(
        assertJakauma(
                jakaumat.get(TyomahdollisuusJakaumaTyyppi.TYON_JATKUVUUS),
                30,
                1,
                TYOMAHDOLLISUUS_JAKAUMA_TYON_JATKUVUUS_KEY,
                0)
            .isPresent());

    // Delete source of import data and re-import
    runSqlScript(TYOMAHDOLLISUUS_DELETE_IMPORT_DATA_SQL);
    runSqlProcedure(TYOMAHDOLLISUUS_IMPORT_DATA_PROCEDURE);
    entity = entityManager.find(Tyomahdollisuus.class, TYOMAHDOLLISUUS_ID);
    assertFalse(entity.isAktiivinen());
  }

  @Test
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe() {
    var suggestions = mahdollisuudetService.getPolkuVaiheSuggestions(Set.of());

    assertThat(suggestions).isEmpty();
  }
}
