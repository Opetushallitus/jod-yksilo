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
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.controller.koski.TestKoskiOauth2Config;
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
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({
  ErrorInfoFactory.class,
  KoskiOauth2Config.class,
  TestKoskiOauth2Config.class,
  KoskiService.class,
  MappingConfig.class,
  ObjectMapper.class,
  KoulutusKokonaisuusService.class,
  KoulutusService.class,
  OsaaminenService.class,
  YksilonOsaaminenService.class,
})
class KoskiServiceTest extends AbstractServiceTest {

  @Autowired private KoskiService koskiService;
  @Autowired private ObjectMapper objectMapper;

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

    var result = KoskiService.join(description, name);

    assertNotNull(result);
    assertEquals(2, result.asMap().size());
    assertLocalizedString(result, "Avoimen opinnot: Lisätietoja", "Öppna studier", null);
  }

  @Test
  void mapKoulutusKokonaisuudet_wrapsIntoKokonaisuusWithKoskiTuontiLahde() throws JacksonException {
    var content = TestUtil.getContentFromFile("koski-response.json", this.getClass());
    var result = koskiService.mapKoulutusKokonaisuudet(objectMapper.readTree(content));

    assertEquals(2, result.size());
    var first = result.getFirst();
    assertNotNull(first.id());
    assertEquals(fi.okm.jod.yksilo.domain.TuontiLahde.KOSKI_TUONTI, first.tuontiLahde());
    assertLocalizedString(
        first.nimi(),
        "Itä-Suomen yliopisto",
        "Östra Finlands Universitet",
        "University of Eastern Finland");
    assertEquals(1, first.koulutukset().size());
    var koulutus = first.koulutukset().iterator().next();
    assertNotNull(koulutus.id());
    assertLocalizedString(
        koulutus.nimi(),
        "Lääketieteen lisensiaatti",
        "Medicine licentiat",
        "Licentiate of Medicine");
    assertNull(koulutus.kuvaus());
    assertEquals(LocalDate.of(2018, 8, 1), koulutus.alkuPvm());
    assertEquals(LocalDate.of(2026, 7, 31), koulutus.loppuPvm());
    assertEquals(Boolean.TRUE, koulutus.osaamisetOdottaaTunnistusta());
    assertEquals(71, koulutus.osasuoritukset().size());
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

    assertEquals(1, result.size());
    var koulutusDto = result.getFirst();
    assertEquals(koulutus1.getId(), koulutusDto.id());
    assertThat(koulutusDto.osaamiset()).hasSize(1);
    assertThat(koulutusDto.osaamisetOdottaaTunnistusta()).isFalse();
    assertThat(koulutusDto.osaamisetTunnistusEpaonnistui()).isFalse();
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
}
