/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.dto.profiili.TmtExportDto.Syy;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Teema;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.external.tmt.model.DescriptionItemExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalPut;
import fi.okm.jod.yksilo.service.tmt.TmtExportService.TmtProfileResult;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TmtExportMappingTest {

  @Test
  void testMapping() {
    Yksilo yksilo = createYksilo();

    TmtProfileResult result = TmtExportService.toTmtProfile(yksilo);
    FullProfileDtoExternalPut profile = result.profile();

    assertNotNull(profile.getEducations());
    assertEquals(
        count(yksilo.getKoulutusKokonaisuudet(), k -> k.getKoulutukset().size()),
        profile.getEducations().size());

    assertNotNull(profile.getEmployments());
    assertEquals(
        count(yksilo.getTyopaikat(), t -> t.getToimenkuvat().size()),
        profile.getEmployments().size());

    assertNotNull(profile.getProjects());
    assertEquals(
        count(yksilo.getTeemat(), t -> t.getToiminnot().size()), profile.getProjects().size());

    profile
        .getEmployments()
        .forEach(
            e -> {
              assertTruncated(e.getEmployer(), 254, "employment.employer");
              assertTruncated(e.getTitle(), 128, "employment.title");
              assertDescriptionTruncated(e.getDescription(), "employment");
            });

    profile
        .getEducations()
        .forEach(
            e -> {
              assertTruncated(e.getDegreeInstitution(), 128, "education.degreeInstitution");
              assertTruncated(e.getCustomDegreeName(), 128, "education.customDegreeName");
              assertDescriptionTruncated(e.getDescription(), "education");
            });

    profile
        .getProjects()
        .forEach(
            p -> {
              assertTruncated(p.getTitle(), 254, "project.title");
              assertDescriptionTruncated(p.getDescription(), "project");
            });
  }

  @Test
  void testMappingWithEmptyData() {
    Yksilo yksilo = new Yksilo(UUID.randomUUID());
    var result = assertDoesNotThrow(() -> TmtExportService.toTmtProfile(yksilo));
    assertNotNull(result.profile());
  }

  @Test
  void skillLimitNotExceededWhenWithinLimit() {
    var yksilo = new Yksilo(UUID.randomUUID());
    var tyopaikka = new Tyopaikka(yksilo, ls("Tyopaikka"));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setKuvaus(ls("Kuvaus"));
    addOsaamiset(toimenkuva, toimenkuva.getOsaamiset(), TmtApiConstants.SKILL_LIMIT);
    tyopaikka.getToimenkuvat().add(toimenkuva);
    yksilo.getTyopaikat().add(tyopaikka);

    var result = TmtExportService.toTmtProfile(yksilo);
    assertTrue(result.warnings().isEmpty());
  }

  @Test
  void skillLimitExceededForToimenkuva() {
    var yksilo = new Yksilo(UUID.randomUUID());
    var tyopaikka = new Tyopaikka(yksilo, ls("Tyopaikka"));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setKuvaus(ls("Kuvaus"));
    addOsaamiset(toimenkuva, toimenkuva.getOsaamiset(), TmtApiConstants.SKILL_LIMIT + 1);
    tyopaikka.getToimenkuvat().add(toimenkuva);
    yksilo.getTyopaikat().add(tyopaikka);

    var result = TmtExportService.toTmtProfile(yksilo);
    assertTrue(result.warnings().contains(Syy.LIIKAA_OSAAMISIA));
  }

  @Test
  void skillLimitExceededForKoulutus() {
    var yksilo = new Yksilo(UUID.randomUUID());
    var kokonaisuus = new KoulutusKokonaisuus(yksilo, ls("Kokonaisuus"));
    var koulutus = new Koulutus(kokonaisuus);
    koulutus.setKuvaus(ls("Kuvaus"));
    addOsaamiset(koulutus, koulutus.getOsaamiset(), TmtApiConstants.SKILL_LIMIT + 1);
    kokonaisuus.getKoulutukset().add(koulutus);
    yksilo.getKoulutusKokonaisuudet().add(kokonaisuus);

    var result = TmtExportService.toTmtProfile(yksilo);
    assertTrue(result.warnings().contains(Syy.LIIKAA_OSAAMISIA));
  }

  @Test
  void skillLimitExceededForToiminto() {
    var yksilo = new Yksilo(UUID.randomUUID());
    var teema = new Teema(yksilo, ls("Teema"));
    var toiminto = new Toiminto(teema);
    toiminto.setKuvaus(ls("Kuvaus"));
    addOsaamiset(toiminto, toiminto.getOsaamiset(), TmtApiConstants.SKILL_LIMIT + 1);
    teema.getToiminnot().add(toiminto);
    yksilo.getTeemat().add(teema);

    var result = TmtExportService.toTmtProfile(yksilo);
    assertTrue(result.warnings().contains(Syy.LIIKAA_OSAAMISIA));
  }

  @Test
  void illegalTextCharactersAreReplacedAndReported() {
    var yksilo = new Yksilo(UUID.randomUUID());
    var tyopaikka = new Tyopaikka(yksilo, ls("Employer" + Character.toString(0x00a0) + "name"));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setNimi(ls("Title\twith emoji " + Character.toString(0x1f642)));
    toimenkuva.setKuvaus(ls("Description" + Character.toString(0x00ad) + "text"));
    tyopaikka.getToimenkuvat().add(toimenkuva);
    yksilo.getTyopaikat().add(tyopaikka);

    var result = TmtExportService.toTmtProfile(yksilo);
    var employment = result.profile().getEmployments().getFirst();

    assertEquals("Employer name", localizedValue(employment.getEmployer()));
    assertEquals("Title with emoji  ", localizedValue(employment.getTitle()));
    assertEquals("Description text", localizedValue(employment.getDescription().getDescription()));
    assertTrue(result.warnings().contains(Syy.KIELLETTYJA_MERKKEJA));
  }

  @Test
  void allowedTextCharactersArePreservedWithoutWarning() {
    var allowedRanges =
        new int[][] {
          {'\n', '\n'}, // Line feed
          {'\r', '\r'}, // Carriage return
          {' ', '~'}, // Printable ASCII
          {0x00a1, 0x00ac}, // ¡ ¢ £ ¤ ¥ ¦ § ¨ © ª « ¬
          {0x00ae, 0x00bf}, // ® ¯ ° ± ² ³ ´ µ ¶ · ¸ ¹ º » ¼ ½ ¾ ¿
          {0x00c0, 0x00d6}, // À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï Ð Ñ Ò Ó Ô Õ Ö
          {0x00d7, 0x00d7}, // ×
          {0x00d8, 0x00f6}, // Ø Ù Ú Û Ü Ý Þ ß à á â ã ä å æ ç è é ê ë ì í î ï ð ñ ò ó ô õ ö
          {0x00f7, 0x00f7}, // ÷
          {0x00f8, 0x00ff}, // ø ù ú û ü ý þ ÿ
          {0x0100, 0x017f}, // Latin Extended-A
          {0x0180, 0x024f}, // Latin Extended-B
          {0x0250, 0x02af}, // IPA Extensions
          {0x20ac, 0x20ac}, // €
          {0x2013, 0x2014} // – —
        };
    var allowed = new StringBuilder();
    for (var range : allowedRanges) {
      IntStream.rangeClosed(range[0], range[1]).forEach(allowed::appendCodePoint);
    }
    var warnings = EnumSet.noneOf(Syy.class);

    assertEquals(allowed.toString(), TmtExportService.sanitize(allowed.toString(), warnings));
    assertTrue(warnings.isEmpty());
  }

  @Test
  void textIsSanitizedBeforeTruncatingSurrogatePairs() {
    var warnings = EnumSet.noneOf(Syy.class);
    var text = "a".repeat(127) + Character.toString(0x1f642);

    var result = TmtExportService.sanitizeAndTruncateValues(Map.of("fi", text), 128, warnings);

    assertEquals("a".repeat(127) + " ", result.get("fi"));
    assertTrue(warnings.contains(Syy.KIELLETTYJA_MERKKEJA));
  }

  @SuppressWarnings("unchecked")
  private static void assertTruncated(Object mapField, int maxLength, String fieldName) {
    assertNotNull(mapField, fieldName + " should not be null");
    var map = (Map<String, String>) mapField;
    map.forEach(
        (lang, value) ->
            assertEquals(
                maxLength,
                value.length(),
                fieldName + "[" + lang + "] should be truncated to " + maxLength));
  }

  @SuppressWarnings("unchecked")
  private static void assertDescriptionTruncated(
      DescriptionItemExternalPut desc, String fieldName) {
    assertNotNull(desc, fieldName + ".description should not be null");
    assertNotNull(desc.getDescription(), fieldName + ".description text should not be null");
    var map = (Map<String, String>) desc.getDescription();
    map.forEach(
        (lang, value) ->
            assertEquals(
                5000,
                value.length(),
                fieldName + ".description[" + lang + "] should be truncated to 5000"));
  }

  private static <T> int count(Collection<T> collection, ToIntFunction<T> weight) {
    return collection.stream().mapToInt(weight).sum();
  }

  @SuppressWarnings("unchecked")
  private static String localizedValue(Object value) {
    return ((Map<String, String>) value).get("fi");
  }

  private static <T extends fi.okm.jod.yksilo.domain.OsaamisenLahde> void addOsaamiset(
      T omistaja, java.util.Collection<YksilonOsaaminen> target, int count) {
    IntStream.range(0, count)
        .forEach(
            i ->
                target.add(
                    new YksilonOsaaminen(
                        omistaja, new Osaaminen(URI.create("urn:osaaminen:" + i)))));
  }

  private static Yksilo createYksilo() {
    var yksilo = new Yksilo(UUID.randomUUID());
    yksilo.setTervetuloapolku(true);
    yksilo
        .getOsaamiset()
        .add(
            new YksilonOsaaminen(
                new MuuOsaaminen(yksilo, Set.of()), new Osaaminen(URI.create("urn:osaaminen:1"))));

    var teema = new Teema(yksilo, ls("Teema 1"));
    var toiminto = new Toiminto(teema);
    toiminto.setAlkuPvm(LocalDate.now());
    toiminto.setLoppuPvm(LocalDate.now().plusYears(1));
    toiminto.setNimi(ls("A".repeat(300)));
    toiminto.setKuvaus(ls("B".repeat(6000)));
    toiminto
        .getOsaamiset()
        .add(new YksilonOsaaminen(toiminto, new Osaaminen(URI.create("urn:osaaminen:2"))));
    yksilo.getOsaamiset().addAll(toiminto.getOsaamiset());
    teema.getToiminnot().add(toiminto);
    yksilo.getTeemat().add(teema);

    var tyopaikka = new Tyopaikka(yksilo, ls("C".repeat(300)));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setAlkuPvm(LocalDate.now());
    toimenkuva.setLoppuPvm(LocalDate.now().plusYears(1));
    toimenkuva.setNimi(ls("D".repeat(200)));
    toimenkuva.setKuvaus(ls("E".repeat(6000)));
    toimenkuva
        .getOsaamiset()
        .add(new YksilonOsaaminen(toimenkuva, new Osaaminen(URI.create("urn:osaaminen:3"))));
    yksilo.getOsaamiset().addAll(toimenkuva.getOsaamiset());
    tyopaikka.getToimenkuvat().add(toimenkuva);
    yksilo.getTyopaikat().add(tyopaikka);

    var koulutusKokonaisuus = new KoulutusKokonaisuus(yksilo, ls("F".repeat(200)));
    var koulutus = new Koulutus(koulutusKokonaisuus);
    koulutus.setAlkuPvm(LocalDate.now());
    koulutus.setLoppuPvm(LocalDate.now().plusYears(1));
    koulutus.setNimi(ls("G".repeat(200)));
    koulutus.setKuvaus(ls("H".repeat(6000)));
    koulutus
        .getOsaamiset()
        .add(new YksilonOsaaminen(koulutus, new Osaaminen(URI.create("urn:osaaminen:4"))));
    yksilo.getOsaamiset().addAll(koulutus.getOsaamiset());
    koulutusKokonaisuus.getKoulutukset().add(koulutus);
    yksilo.getKoulutusKokonaisuudet().add(koulutusKokonaisuus);

    return yksilo;
  }
}
