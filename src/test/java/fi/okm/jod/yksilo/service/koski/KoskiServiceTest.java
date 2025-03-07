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

    assertEquals(1, koskiData.size());
    var koulutusDto = koskiData.getFirst();
    assertNull(koulutusDto.id());
    assertEquals("Jyväskylän normaalikoulu", koulutusDto.nimi().get(Kieli.FI));
    assertEquals("Jyväskylän normaalikoulu", koulutusDto.nimi().get(Kieli.EN));
    assertEquals("Jyväskylän normaalikoulu", koulutusDto.nimi().get(Kieli.SV));
    assertEquals(
        "Aikuisten perusopetuksen oppimäärän alkuvaihe", koulutusDto.kuvaus().get(Kieli.FI));
    assertEquals(
        "Introductory phase to basic education for adults  syllabus",
        koulutusDto.kuvaus().get(Kieli.EN));
    assertEquals(
        "Inledningsskedet i den grundläggande utbildningen för vuxna",
        koulutusDto.kuvaus().get(Kieli.SV));
    assertEquals(LocalDate.of(2006, 1, 1), koulutusDto.alkuPvm());
    assertNull(koulutusDto.loppuPvm());
    assertNull(koulutusDto.osaamiset());
  }

  @Test
  void testGetLocalizedKuvaus_withDescriptionsAndNames() {
    var description = createLocalizedDescription();
    var name = createLocalizedName();

    var result = KoskiService.getLocalizedKuvaus(description, name);

    assertNotNull(result);
    assertEquals(
        "Avoimen opinnot: Aducate/A1999/Avoin yo, kasv./Kasvatustiede/Kasvat. appr",
        result.get(Kieli.FI));
    assertEquals(
        "Open University studies: Aducate/A1999/Open University, Education/Educational Science/Educational Approach",
        result.get(Kieli.EN));
    assertEquals(
        "Öppna studier: Aducate/A1999/Öppna universitetet, ped./Pedagogik/Pedag. appr",
        result.get(Kieli.SV));
  }

  private static LocalizedString createLocalizedName() {
    return new LocalizedString(
        Map.of(
            Kieli.FI,
            "Aducate/A1999/Avoin yo, kasv./Kasvatustiede/Kasvat. appr",
            Kieli.EN,
            "Aducate/A1999/Open University, Education/Educational Science/Educational Approach",
            Kieli.SV,
            "Aducate/A1999/Öppna universitetet, ped./Pedagogik/Pedag. appr"));
  }

  private static LocalizedString createLocalizedDescription() {
    return new LocalizedString(
        Map.of(
            Kieli.FI,
            "Avoimen opinnot",
            Kieli.EN,
            "Open University studies",
            Kieli.SV,
            "Öppna studier"));
  }

  @Test
  void testGetLocalizedKuvaus_nullNameValues() {
    var description = createLocalizedDescription();
    LocalizedString name = null;

    var result = KoskiService.getLocalizedKuvaus(description, name);

    assertNotNull(result);
    assertEquals("Avoimen opinnot", result.get(Kieli.FI));
    assertEquals("Open University studies", result.get(Kieli.EN));
    assertEquals("Öppna studier", result.get(Kieli.SV));
  }

  @Test
  void testGetLocalizedKuvaus_emptyValues() {
    var description = createLocalizedDescription();
    var name = new LocalizedString(Map.of(Kieli.FI, "", Kieli.EN, ""));

    var result = KoskiService.getLocalizedKuvaus(description, name);

    assertNull(result);
  }
}
