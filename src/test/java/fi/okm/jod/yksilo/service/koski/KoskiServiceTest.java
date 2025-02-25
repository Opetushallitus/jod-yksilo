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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.testutil.TestUtil;
import java.time.LocalDate;
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
    var koskiData = koskiService.getKoulutusData(objectMapper.readTree(content), null);

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
}
