/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jayway.jsonpath.JsonPath;
import fi.okm.jod.yksilo.domain.Kieli;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

class KoskiServiceTest {

  private final KoskiService koskiService;

  public KoskiServiceTest() {
    this.koskiService =
        new KoskiService(RestClient.builder(), new MappingJackson2HttpMessageConverter());
  }

  @Test
  void getKoulutusData() {
    var content = getContentFromFile("koski-response2.json", this.getClass());
    var koskiData = koskiService.getKoulutusData(JsonPath.parse(content).json(), null);

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

  private static String getContentFromFile(String filename, Class<?> clazz) {
    try (var inputStream = clazz.getResourceAsStream(filename)) {
      assert inputStream != null;
      return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new RuntimeException("Could not read file: " + filename, e);
    }
  }
}
