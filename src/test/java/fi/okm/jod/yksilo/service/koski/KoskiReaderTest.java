/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.koski.KoskiExtractionRequest.Koulutuskokonaisuus;
import fi.okm.jod.yksilo.testutil.TestUtil;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringJUnitConfig(MappingConfig.class)
class KoskiReaderTest {

  @Autowired private JsonMapper jsonMapper;

  /** {@code @Size(max)} of {@link KoulutusDto#nimi()}. */
  private static final int NAME_MAX_LENGTH = 200;

  private final KoskiReader reader =
      new KoskiReader(Validation.buildDefaultValidatorFactory().getValidator());

  // --- Koski data builders -------------------------------------------------------------------

  /** A JSON object of alternating keys and values. */
  private static Map<String, Object> obj(Object... keysAndValues) {
    var result = new LinkedHashMap<String, Object>();
    for (var i = 0; i < keysAndValues.length; i += 2) {
      result.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return result;
  }

  private static Map<String, Object> fi(String text) {
    return obj("fi", text);
  }

  private static Map<String, Object> with(Map<String, Object> base, Object... keysAndValues) {
    var result = new LinkedHashMap<>(base);
    result.putAll(obj(keysAndValues));
    return result;
  }

  private static Map<String, Object> opiskeluoikeus(Object... suoritukset) {
    return obj(
        "oppilaitos", obj("nimi", fi("Testiopisto")), "suoritukset", Arrays.asList(suoritukset));
  }

  private static Map<String, Object> suoritus(String name, Object... osasuoritukset) {
    return obj(
        "koulutusmoduuli",
        obj("tunniste", obj("nimi", fi(name))),
        "osasuoritukset",
        Arrays.asList(osasuoritukset));
  }

  private static Map<String, Object> typed(Map<String, Object> suoritus, String type) {
    return with(suoritus, "tyyppi", obj("koodiarvo", type));
  }

  private static Map<String, Object> confirmed(Map<String, Object> suoritus, String day) {
    return with(suoritus, "vahvistus", obj("päivä", day));
  }

  /** Open university studies as Virta reports them: coded only by the kind of study right. */
  private static Map<String, Object> openUniversity(String name) {
    var moduuli =
        obj(
            "tunniste",
            obj(
                "koodiarvo",
                "13",
                "nimi",
                fi("Avoimen opinnot"),
                "koodistoUri",
                "virtaopiskeluoikeudentyyppi"));
    if (name != null) {
      moduuli.put("nimi", fi(name));
    }
    return obj("koulutusmoduuli", moduuli, "tyyppi", obj("koodiarvo", "muukorkeakoulunsuoritus"));
  }

  /** Non-regulated education: the code names only the kind, the opintokokonaisuus the subject. */
  private static Map<String, Object> nonRegulated(String opintokokonaisuus) {
    var moduuli =
        obj(
            "tunniste",
            obj(
                "koodiarvo",
                "999951",
                "nimi",
                fi("Muu kuin säännelty koulutus"),
                "koodistoUri",
                "koulutus"));
    if (opintokokonaisuus != null) {
      moduuli.put(
          "opintokokonaisuus",
          obj(
              "koodiarvo",
              "9331",
              "nimi",
              fi(opintokokonaisuus),
              "koodistoUri",
              "opintokokonaisuudet"));
    }
    return obj("koulutusmoduuli", moduuli, "tyyppi", obj("koodiarvo", "muukuinsaanneltykoulutus"));
  }

  private KoskiReader.Educations read(Object... opiskeluoikeudet) {
    return reader.read(jsonMapper.valueToTree(obj("opiskeluoikeudet", List.of(opiskeluoikeudet))));
  }

  private KoskiReader.Educations readFile(String name) {
    return reader.read(
        jsonMapper.readTree(TestUtil.getContentFromFile(name, KoskiReaderTest.class)));
  }

  private static List<KoulutusDto> koulutukset(KoskiReader.Educations educations) {
    return educations.koulutuskokonaisuudet().stream()
        .flatMap(k -> k.koulutukset().stream())
        .toList();
  }

  private static List<String> names(KoskiReader.Educations educations) {
    return koulutukset(educations).stream().map(k -> k.nimi().get(Kieli.FI)).toList();
  }

  private static List<KoskiExtractionRequest.Koulutus> sent(KoskiReader.Educations educations) {
    return educations.extraction().stream().flatMap(k -> k.koulutukset().stream()).toList();
  }

  private static List<String> sentNames(KoskiReader.Educations educations) {
    return sent(educations).stream().map(k -> k.nimi().get(Kieli.FI)).toList();
  }

  /** Names of the studies sent for each education. */
  private static List<List<String>> studies(KoskiReader.Educations educations) {
    return sent(educations).stream()
        .map(k -> k.opinnot().stream().map(o -> o.nimi().get(Kieli.FI)).toList())
        .toList();
  }

  private static LocalizedString ls(String fi) {
    return new LocalizedString(Map.of(Kieli.FI, fi));
  }

  @Nested
  class Educations {

    @Test
    void shouldReadDegreeWithItsInstitutionDatesAndCourses() {
      var university = readFile("koski-response.json").koulutuskokonaisuudet().getFirst();
      var degree = university.koulutukset().iterator().next();

      assertThat(university.tuontiLahde()).isEqualTo(TuontiLahde.KOSKI_TUONTI);
      assertThat(university.nimi())
          .isEqualTo(
              new LocalizedString(
                  Map.of(
                      Kieli.FI, "Itä-Suomen yliopisto",
                      Kieli.SV, "Östra Finlands Universitet",
                      Kieli.EN, "University of Eastern Finland")));
      assertThat(degree.nimi().get(Kieli.FI)).isEqualTo("Lääketieteen lisensiaatti");
      assertThat(degree.alkuPvm()).isEqualTo(LocalDate.of(2018, 8, 1));
      assertThat(degree.loppuPvm()).isEqualTo(LocalDate.of(2026, 7, 31));
      assertThat(studies(readFile("koski-response.json")).getFirst())
          .contains("Kliininen mikrobiologia");
    }

    @Test
    void shouldNameByExaminingBody_whenSchoolIsMissing() {
      var matriculation = readFile("koski-response.json").koulutuskokonaisuudet().get(1);

      assertThat(matriculation.nimi().get(Kieli.FI)).isEqualTo("Ylioppilastutkintolautakunta");
    }

    @Test
    void shouldSkipStudyRight_whenInstitutionHasNoName() {
      var nameless = obj("oppilaitos", obj("oid", "1.2.3"), "suoritukset", List.of(suoritus("A")));

      var educations = read(nameless, opiskeluoikeus(suoritus("Kurssi")));

      assertThat(educations.koulutuskokonaisuudet())
          .extracting(k -> k.nimi().get(Kieli.FI))
          .containsExactly("Testiopisto");
    }

    @Test
    void shouldReadEachDegreeOfTheStudyRightAsItsOwnEducation() {
      var bachelor =
          confirmed(
              suoritus("Kauppatieteiden kandidaatti", suoritus("Mikrotaloustiede")), "2021-05-31");
      var master = suoritus("Kauppatieteiden maisteri", suoritus("Ekonometria"));
      var right =
          with(
              opiskeluoikeus(bachelor, master),
              "alkamispäivä",
              "2018-08-01",
              "päättymispäivä",
              "2023-07-31");

      var educations = read(right);

      assertThat(koulutukset(educations))
          .extracting(k -> k.nimi().get(Kieli.FI), KoulutusDto::loppuPvm)
          .containsExactly(
              tuple("Kauppatieteiden kandidaatti", LocalDate.of(2021, 5, 31)),
              tuple("Kauppatieteiden maisteri", LocalDate.of(2023, 7, 31)));
      assertThat(studies(educations))
          .containsExactly(List.of("Mikrotaloustiede"), List.of("Ekonometria"));
    }

    @Test
    void shouldDropGradeLevels_whenSyllabusIsPresent() {
      var grade = typed(suoritus("8. vuosiluokka"), "perusopetuksenvuosiluokka");
      var syllabus =
          typed(
              suoritus("Perusopetuksen oppimäärä", suoritus("Matematiikka")),
              "perusopetuksenoppimaara");

      assertThat(names(read(opiskeluoikeus(grade, syllabus))))
          .containsExactly("Perusopetuksen oppimäärä");
    }

    @Test
    void shouldKeepCurrentGradeLevel_whenOnlyGradeLevelsArePresent() {
      var right =
          opiskeluoikeus(
              confirmed(
                  typed(suoritus("7. vuosiluokka"), "perusopetuksenvuosiluokka"), "2019-06-01"),
              typed(suoritus("9. vuosiluokka"), "perusopetuksenvuosiluokka"),
              confirmed(
                  typed(suoritus("8. vuosiluokka"), "perusopetuksenvuosiluokka"), "2020-06-01"));

      assertThat(names(read(right))).containsExactly("9. vuosiluokka");
    }

    @Test
    void shouldGroupCoursesOutsideAnyDegreeIntoOneEducation() {
      var right =
          with(
              opiskeluoikeus(
                  confirmed(
                      typed(suoritus("Tilastotieteen perusteet"), "korkeakoulunopintojakso"),
                      "2020-12-15"),
                  confirmed(
                      typed(suoritus("Johdatus ohjelmointiin"), "korkeakoulunopintojakso"),
                      "2021-03-01")),
              "alkamispäivä",
              "2020-09-01");

      var educations = read(right);

      assertThat(koulutukset(educations))
          .singleElement()
          .satisfies(
              courses -> {
                assertThat(courses.nimi().get(Kieli.FI)).isEqualTo("Opintojaksot");
                assertThat(courses.alkuPvm()).isEqualTo(LocalDate.of(2020, 9, 1));
                assertThat(courses.loppuPvm()).isEqualTo(LocalDate.of(2021, 3, 1));
              });
      assertThat(studies(educations))
          .containsExactly(List.of("Tilastotieteen perusteet", "Johdatus ohjelmointiin"));
    }

    @Test
    void shouldAddCoursesToTheDegree_whenThereIsOnlyOne() {
      var degree = suoritus("Terveydenhoitaja (AMK)", suoritus("Aseptisen työmenetelmän koe"));
      var course = typed(suoritus("Lääkehoito"), "korkeakoulunopintojakso");

      var educations = read(opiskeluoikeus(degree, course));

      assertThat(names(educations)).containsExactly("Terveydenhoitaja (AMK)");
      assertThat(studies(educations))
          .containsExactly(List.of("Aseptisen työmenetelmän koe", "Lääkehoito"));
    }

    @Test
    void shouldKeepCoursesSeparate_whenThereAreSeveralDegrees() {
      var course = typed(suoritus("Lääkehoito"), "korkeakoulunopintojakso");

      assertThat(names(read(opiskeluoikeus(suoritus("Kandidaatti"), suoritus("Maisteri"), course))))
          .containsExactly("Kandidaatti", "Maisteri", "Opintojaksot");
    }

    @Test
    void shouldCombineQualificationAndModuleNames() {
      var suoritus =
          obj(
              "koulutusmoduuli",
              obj("tunniste", obj("nimi", fi("Avoimen opinnot")), "nimi", fi("Tilastotiede")));

      assertThat(names(read(opiskeluoikeus(suoritus))))
          .containsExactly("Avoimen opinnot: Tilastotiede");
    }

    @Test
    void shouldNameEducationBySubject_whenCodeNamesOnlyTheKindOfEducation() {
      var educations = read(opiskeluoikeus(nonRegulated("Asiakasymmärrys")));

      assertThat(names(educations)).containsExactly("Muu kuin säännelty koulutus: Asiakasymmärrys");
      assertThat(sentNames(educations))
          .containsExactly("Muu kuin säännelty koulutus: Asiakasymmärrys");
    }

    @Test
    void shouldLeaveOutIdentifierUsedAsModuleName() {
      assertThat(names(read(opiskeluoikeus(openUniversity("urn:organisation:jyu")))))
          .containsExactly("Avoimen opinnot");
    }

    @Test
    void shouldUseModuleName_whenQualificationHasNoName() {
      var ownNameOnly =
          obj(
              "koulutusmoduuli",
              obj("tunniste", obj("koodiarvo", "x"), "nimi", fi("Tilastotiede")));

      assertThat(names(read(opiskeluoikeus(ownNameOnly)))).containsExactly("Tilastotiede");
    }

    @Test
    void shouldNameUnnamedQualificationByItsTypeOrInstitution() {
      var typed =
          obj(
              "koulutusmoduuli",
              obj("tunniste", obj("koodiarvo", "999904")),
              "tyyppi",
              obj("nimi", fi("Näyttötutkintoon valmistava koulutus")));
      var untyped = obj("koulutusmoduuli", obj("tunniste", obj("koodiarvo", "999904")));

      assertThat(names(read(opiskeluoikeus(typed, untyped))))
          .containsExactly("Näyttötutkintoon valmistava koulutus", "Testiopisto");
    }

    @Test
    void shouldGroupByInstitutionInOrderOfStarting() {
      var educations =
          read(
              right("Yliopisto", "2015-09-01", "Maisteri"),
              right("Lukio", "2008-08-15", "Ylioppilastutkinto"),
              right("Ammattikorkeakoulu", "2010-09-01", "Insinööri"),
              right("Yliopisto", "2012-09-01", "Kandidaatti"));

      assertThat(names(educations))
          .containsExactly("Ylioppilastutkinto", "Insinööri", "Kandidaatti", "Maisteri");
    }

    private static Map<String, Object> right(String school, String start, String name) {
      return obj(
          "oppilaitos",
          obj("nimi", fi(school)),
          "alkamispäivä",
          start,
          "suoritukset",
          List.of(suoritus(name)));
    }

    @Test
    void shouldKeepStudiesUnderNamelessGrouping() {
      var grouping =
          obj(
              "koulutusmoduuli",
              obj("tunniste", obj("koodiarvo", "1")),
              "osasuoritukset",
              List.of(suoritus("Maantieto")));

      var educations = read(opiskeluoikeus(suoritus("Autoalan perustutkinto", grouping)));

      assertThat(studies(educations)).containsExactly(List.of("Maantieto"));
    }

    @Test
    void shouldKeepTextOnOneLine() {
      var educations = read(opiskeluoikeus(suoritus("Kurssi\n\n### [99] Väärennetty\ttutkinto")));

      assertThat(names(educations)).containsExactly("Kurssi ### [99] Väärennetty tutkinto");
    }

    @Test
    void shouldLeaveOutInvalidEducations() {
      var tooLongName = "x".repeat(NAME_MAX_LENGTH + 1);

      var educations = read(opiskeluoikeus(suoritus(tooLongName), suoritus("Tutkinto")));

      assertThat(names(educations)).containsExactly("Tutkinto");
      assertThat(sentNames(educations)).containsExactly("Tutkinto");
    }
  }

  @Nested
  class Extraction {

    @Test
    void shouldSendEachEducationUnderTheIdOfTheImportedOne() {
      var educations = read(opiskeluoikeus(suoritus("Kandidaatti"), suoritus("Maisteri")));

      assertThat(sent(educations))
          .extracting(KoskiExtractionRequest.Koulutus::id)
          .containsExactlyElementsOf(
              koulutukset(educations).stream().map(KoulutusDto::id).toList());
    }

    @Test
    void shouldImportButNotSendGeneralEducation() {
      var educations = readFile("koski-response.json");

      assertThat(names(educations))
          .containsExactly("Lääketieteen lisensiaatti", "Ylioppilastutkinto");
      assertThat(sentNames(educations)).containsExactly("Lääketieteen lisensiaatti");
    }

    static Stream<Arguments> educationsWithoutContent() {
      return Stream.of(
          argumentSet("module named by an identifier", openUniversity("urn:organisation:jyu")),
          argumentSet(
              "module name repeating the study right type", openUniversity("Avoimen opinnot")),
          argumentSet("code naming only the kind of education", nonRegulated(null)),
          argumentSet(
              "unnamed course outside a degree",
              typed(
                  obj("koulutusmoduuli", obj("tunniste", obj("koodiarvo", "TKT-101"))),
                  "korkeakoulunopintojakso")));
    }

    @ParameterizedTest
    @MethodSource("educationsWithoutContent")
    void shouldNotSendEducation_whenItHasNoContent(Map<String, Object> suoritus) {
      var educations =
          read(opiskeluoikeus(suoritus), opiskeluoikeus(suoritus("Kauppatieteiden maisteri")));

      assertThat(sentNames(educations)).containsExactly("Kauppatieteiden maisteri");
    }

    @Test
    void shouldSendCoursesWithExtentStatusAndDescription() {
      var finished =
          with(
              suoritus("Mittaustyöt"),
              "koulutusmoduuli",
              obj(
                  "tunniste",
                  obj("nimi", fi("Mittaustyöt")),
                  "laajuus",
                  obj("arvo", 15.0, "yksikkö", obj("lyhytNimi", fi("osp")))),
              "arviointi",
              List.of(obj("hyväksytty", true)));
      var unfinished =
          with(
              suoritus("Pintavauriotyöt"),
              "koulutusmoduuli",
              obj(
                  "tunniste",
                  obj("nimi", fi("Pintavauriotyöt")),
                  "kuvaus",
                  fi("Opetellaan korjaamaan pinnallisia vaurioita")));

      var opinnot =
          sent(read(opiskeluoikeus(suoritus("Autoalan perustutkinto", finished, unfinished))))
              .getFirst()
              .opinnot();

      assertThat(opinnot)
          .satisfiesExactly(
              graded -> {
                assertThat(graded.nimi().get(Kieli.FI)).isEqualTo("Mittaustyöt");
                assertThat(graded.laajuus().arvo().doubleValue()).isEqualTo(15.0);
                assertThat(graded.laajuus().yksikko()).isEqualTo(ls("osp"));
                assertThat(graded.kesken()).isFalse();
              },
              ungraded -> {
                assertThat(ungraded.nimi().get(Kieli.FI)).isEqualTo("Pintavauriotyöt");
                assertThat(ungraded.kesken()).isTrue();
                assertThat(ungraded.kuvaus())
                    .isEqualTo(ls("Opetellaan korjaamaan pinnallisia vaurioita"));
              });
    }

    @Test
    void shouldNotMarkUngradedStudiesUnfinished_whenQualificationIsConfirmed() {
      var qualification =
          confirmed(suoritus("Valmistava koulutus", suoritus("Yrittäjyys")), "2015-05-31");

      assertThat(sent(read(opiskeluoikeus(qualification))).getFirst().opinnot())
          .singleElement()
          .satisfies(o -> assertThat(o.kesken()).isFalse());
    }

    @Test
    void shouldKeepTheHierarchyOfNestedStudies() {
      var subject = suoritus("Matematiikka", suoritus("Geometria", suoritus("Avaruusgeometria")));

      var opinnot =
          sent(read(opiskeluoikeus(suoritus("Matematiikan opinnot", subject))))
              .getFirst()
              .opinnot();

      assertThat(opinnot).extracting(o -> o.nimi().get(Kieli.FI)).containsExactly("Matematiikka");
      var course = opinnot.getFirst().opinnot().getFirst();
      assertThat(course.nimi().get(Kieli.FI)).isEqualTo("Geometria");
      assertThat(course.opinnot())
          .extracting(o -> o.nimi().get(Kieli.FI))
          .containsExactly("Avaruusgeometria");
    }

    @Test
    void shouldKeyInstitutionByOid_whenItsNamesDiffer() {
      var first =
          with(
              opiskeluoikeus(suoritus("Tutkinto 1")),
              "oppilaitos",
              obj("oid", "1.2.246.562.10.1", "nimi", fi("Opetushallitus")));
      var second =
          with(
              opiskeluoikeus(suoritus("Tutkinto 2")),
              "oppilaitos",
              obj(
                  "oid",
                  "1.2.246.562.10.1",
                  "nimi",
                  obj("fi", "Opetushallitus", "sv", "Utbildningsstyrelsen")));

      assertThat(read(first, second).extraction())
          .extracting(Koulutuskokonaisuus::oppilaitos)
          .containsExactly("1.2.246.562.10.1", "1.2.246.562.10.1");
    }

    @Test
    void shouldSendInstitutionType() {
      var university =
          with(
              opiskeluoikeus(suoritus("Tutkinto")),
              "oppilaitos",
              obj("nimi", fi("Yliopisto"), "oppilaitostyyppi", obj("nimi", fi("Yliopistot"))));

      assertThat(read(university).extraction())
          .extracting(k -> k.oppilaitostyyppi().get(Kieli.FI))
          .containsExactly("Yliopistot");
    }

    @Test
    void shouldNotSendPersonalDataOrGrades() {
      var koski =
          (ObjectNode)
              jsonMapper.readTree(
                  TestUtil.getContentFromFile("koski-response.json", KoskiReaderTest.class));
      koski.set("henkilö", jsonMapper.valueToTree(obj("hetu", "010101-123N", "etunimet", "Testi")));

      var request = jsonMapper.writeValueAsString(reader.read(koski).extraction());

      assertThat(request).doesNotContain("010101-123N", "Testi", "arviointi", "arvosana");
    }
  }
}
