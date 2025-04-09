/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.controller.koski.TestKoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.testutil.TestUtil;
import fi.okm.jod.yksilo.validation.Limits;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@Sql(value = "/data/osaaminen.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({
  ErrorInfoFactory.class,
  KoskiOAuth2Config.class,
  TestKoskiOAuth2Config.class,
  KoskiService.class,
  MappingConfig.class,
  ObjectMapper.class,
})
class KoskiServiceTest extends AbstractServiceTest {

  @Autowired private KoskiService koskiService;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void getKoulutusData() throws JsonProcessingException {
    var content = TestUtil.getContentFromFile("koski-response.json", this.getClass());
    var koskiData = koskiService.getKoulutusData(objectMapper.readTree(content));

    assertEquals(2, koskiData.size());
    var koulutusDto = koskiData.getFirst();
    assertNull(koulutusDto.id());
    assertLocalizedString(
        koulutusDto.nimi(),
        "Itä-Suomen yliopisto",
        "Östra Finlands Universitet",
        "University of Eastern Finland");
    assertLocalizedString(
        koulutusDto.kuvaus(),
        "Lääketieteen lisensiaatti",
        "Medicine licentiat",
        "Licentiate of Medicine");
    assertEquals(LocalDate.of(2018, 8, 1), koulutusDto.alkuPvm());
    assertEquals(LocalDate.of(2026, 7, 31), koulutusDto.loppuPvm());
    assertNull(koulutusDto.osaamiset());
    assertEquals(71, koulutusDto.osasuoritukset().size());

    var koulutusDto2 = koskiData.get(1);
    assertNull(koulutusDto2.id());
    assertLocalizedString(
        koulutusDto2.nimi(),
        "Ylioppilastutkintolautakunta",
        "Studentexamensnämnden",
        "The Matriculation Examination Board");
    assertLocalizedString(
        koulutusDto2.kuvaus(), "Ylioppilastutkinto", "Studentexamen", "Matriculation Examination");
    assertNull(koulutusDto2.alkuPvm());
    assertNull(koulutusDto2.loppuPvm());
    assertNull(koulutusDto2.osaamiset());
    assertTrue(koulutusDto2.osasuoritukset().isEmpty());
  }

  private static void assertLocalizedString(
      LocalizedString localizedString,
      String expectedFinnish,
      String expectedSwedish,
      String expectedEnglish) {
    if (expectedFinnish == null) {
      assertNull(localizedString.get(Kieli.FI));
    } else {
      assertEquals(expectedFinnish, localizedString.get(Kieli.FI));
    }
    if (expectedSwedish == null) {
      assertNull(localizedString.get(Kieli.SV));
    } else {
      assertEquals(expectedSwedish, localizedString.get(Kieli.SV));
    }
    if (expectedEnglish == null) {
      assertNull(localizedString.get(Kieli.EN));
    } else {
      assertEquals(expectedEnglish, localizedString.get(Kieli.EN));
    }
  }

  @Test
  void testGetLocalizedKuvaus() {
    var description =
        new LocalizedString(Map.of(Kieli.FI, "Avoimen opinnot", Kieli.SV, "Öppna studier"));
    var name = new LocalizedString(Map.of(Kieli.FI, "Lisätietoja", Kieli.EN, "Additional info"));

    var result = KoskiService.getLocalizedKuvaus(description, name);

    assertNotNull(result);
    assertEquals(2, result.asMap().size());
    assertLocalizedString(result, "Avoimen opinnot: Lisätietoja", "Öppna studier", null);
  }

  @Test
  void testGetOsaamisetIdentified_validData() {
    var em = entityManager.getEntityManager();

    var yksilo = em.find(Yksilo.class, user.getId());
    var koulutusKokonaisuus = new KoulutusKokonaisuus(yksilo, ls("Koulu1"));
    entityManager.persist(koulutusKokonaisuus);
    var koulutus1 = createKoulutus1(entityManager, koulutusKokonaisuus, yksilo);
    var koulutus2 = createKoulutus2(entityManager, koulutusKokonaisuus);

    var result =
        koskiService.getOsaamisetIdentified(user, List.of(koulutus1.getId(), koulutus2.getId()));

    assertEquals(2, result.size());
    for (var koulutusDto : result) {
      if (koulutusDto.id().equals(koulutus1.getId())) {
        assertThat(koulutusDto.osaamiset()).hasSize(1);
        assertThat(koulutusDto.osaamisetOdottaaTunnistusta()).isFalse();
        assertThat(koulutusDto.osaamisetTunnistusEpaonnistui()).isFalse();

      } else {
        assertThat(koulutusDto.osaamiset()).isEmpty();
        assertThat(koulutusDto.osaamisetOdottaaTunnistusta()).isTrue();
        assertThat(koulutusDto.osaamisetTunnistusEpaonnistui()).isFalse();
      }
    }
  }

  private static Koulutus createKoulutus1(
      TestEntityManager entityManager, KoulutusKokonaisuus koulutusKokonaisuus, Yksilo yksilo) {
    var koulutus1 = new Koulutus(koulutusKokonaisuus);
    koulutus1.setNimi(new LocalizedString(Map.of(Kieli.FI, "Koulutus 1")));
    koulutus1.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.DONE);
    var tyopaikka = new Tyopaikka(yksilo, ls("Testi"));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setNimi(ls("Toimenkuva"));
    koulutus1
        .getOsaamiset()
        .add(new YksilonOsaaminen(toimenkuva, entityManager.find(Osaaminen.class, 1L)));
    koulutus1 = entityManager.persist(koulutus1);
    return koulutus1;
  }

  private static Koulutus createKoulutus2(
      TestEntityManager entityManager, KoulutusKokonaisuus koulutusKokonaisuus) {
    var koulutus2 = new Koulutus(koulutusKokonaisuus);
    koulutus2.setNimi(new LocalizedString(Map.of(Kieli.FI, "Koulutus 2")));
    koulutus2.setOsaamisenTunnistusStatus(OsaamisenTunnistusStatus.WAIT);
    koulutus2 = entityManager.persist(koulutus2);
    return koulutus2;
  }

  @Test
  void testGetOsaamisetIdentified_emptyResult() {
    var result = koskiService.getOsaamisetIdentified(user, List.of());

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetOsaamisetIdentified_exceedsUuidLimit() {
    var uuids =
        Stream.generate(UUID::randomUUID)
            .limit(Limits.KOULUTUSKOKONAISUUS * Limits.KOULUTUS_PER_KOKONAISUUS + 1)
            .toList();

    var exception =
        assertThrows(
            KoskiServiceException.class, () -> koskiService.getOsaamisetIdentified(user, uuids));
    assertThat(exception.getMessage()).contains("exceeded");
  }

  @Test
  void testGetOsaamisetIdentified_unauthorizedAccess() {
    var em = entityManager.getEntityManager();

    var yksilo = em.find(Yksilo.class, user.getId());
    var koulutusKokonaisuus1 = new KoulutusKokonaisuus(yksilo, ls("Koulu1"));
    entityManager.persist(koulutusKokonaisuus1);
    var koulutus1 = createKoulutus1(entityManager, koulutusKokonaisuus1, yksilo);

    var koulutusKokonaisuus2 =
        new KoulutusKokonaisuus(em.find(Yksilo.class, user2.getId()), ls("Koulu2"));
    entityManager.persist(koulutusKokonaisuus2);
    var koulutus2 = createKoulutus2(entityManager, koulutusKokonaisuus2);

    var koulutusIds = List.of(koulutus1.getId(), koulutus2.getId());
    assertThat(
            assertThrows(
                    KoskiServiceException.class,
                    () -> koskiService.getOsaamisetIdentified(user, koulutusIds))
                .getMessage())
        .contains("another user");
  }
}
