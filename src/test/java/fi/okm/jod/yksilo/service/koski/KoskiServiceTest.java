/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KoskiServiceTest {

  private final KoskiService koskiService;
  private final ObjectMapper objectMapper;

  public KoskiServiceTest() {
    this.objectMapper = new ObjectMapper();
    this.koskiService = new KoskiService();
  }

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
}
