/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CvResponseMapperTest {

  private static final Kieli KIELI = Kieli.FI;
  private static final LocalDate DATE = LocalDate.of(2020, 1, 1);

  private static CvResponseMapper mapper;

  @BeforeAll
  static void setUp() {
    var factory = Validation.buildDefaultValidatorFactory();
    mapper = new CvResponseMapper(factory.getValidator());
  }

  private static CvTehtavaDto.Tulos map(CvResponse response) {
    return mapper.toTulos(response, KIELI);
  }

  private static CvResponse withEducation(CvResponse.Education... items) {
    return new CvResponse(null, List.of(items), null);
  }

  private static CvResponse withWork(CvResponse.WorkExperience... items) {
    return new CvResponse(List.of(items), null, null);
  }

  private static CvResponse withActivity(CvResponse.Activity... items) {
    return new CvResponse(null, null, List.of(items));
  }

  private static CvResponse.Position position(
      String title, LocalDate start, LocalDate end, String description) {
    return new CvResponse.Position(title, start, end, null, description, null);
  }

  private static CvResponse.Degree degree(
      String name, LocalDate start, LocalDate end, String details) {
    return new CvResponse.Degree(name, start, end, details);
  }

  @Test
  void shouldMapValidResponse() {
    var response =
        new CvResponse(
            List.of(
                new CvResponse.WorkExperience(
                    "Company", List.of(position("Title", DATE, null, "Desc")))),
            List.of(
                new CvResponse.Education(
                    "University", List.of(degree("Degree", DATE, null, "Details")))),
            List.of(new CvResponse.Activity("Hobby", "Name", "Description", DATE, null)));

    var tulos = map(response);

    assertThat(tulos.koulutuskokonaisuudet()).hasSize(1);
    assertThat(tulos.tyopaikat()).hasSize(1);
    assertThat(tulos.teemat()).hasSize(1);
  }

  @Test
  void shouldMapLegacyFlatResponse() {
    var response =
        new CvResponse(
            List.of(
                new CvResponse.WorkExperience(
                    "Company", null, "Title", DATE, null, null, "Desc", null)),
            List.of(new CvResponse.Education("University", null, "Degree", DATE, null, "Details")),
            null);

    var tulos = map(response);

    assertThat(tulos.tyopaikat())
        .singleElement()
        .satisfies(it -> assertThat(it.toimenkuvat()).hasSize(1));
    assertThat(tulos.koulutuskokonaisuudet())
        .singleElement()
        .satisfies(it -> assertThat(it.koulutukset()).hasSize(1));
  }

  @Test
  void shouldMapAllPositionsOfEmployer() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(
                    "Company",
                    List.of(
                        position("Current", LocalDate.of(2022, 3, 1), null, "Desc"),
                        position(
                            "Previous",
                            LocalDate.of(2019, 6, 1),
                            LocalDate.of(2022, 2, 28),
                            "Desc")))));

    assertThat(tulos.tyopaikat())
        .singleElement()
        .satisfies(it -> assertThat(it.toimenkuvat()).hasSize(2));
  }

  @Test
  void shouldMapAllDegreesOfInstitution() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education(
                    "University",
                    List.of(
                        degree("M.S.", LocalDate.of(2013, 1, 1), LocalDate.of(2015, 12, 31), null),
                        degree(
                            "B.S.", LocalDate.of(2009, 1, 1), LocalDate.of(2013, 12, 31), null)))));

    assertThat(tulos.koulutuskokonaisuudet())
        .singleElement()
        .satisfies(it -> assertThat(it.koulutukset()).hasSize(2));
  }

  @Test
  void shouldKeepValidPositionsAndExcludeInvalid() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(
                    "Company",
                    List.of(
                        position(null, DATE, null, "Desc"),
                        position("Valid", DATE, null, "Desc")))));

    assertThat(tulos.tyopaikat())
        .singleElement()
        .satisfies(it -> assertThat(it.toimenkuvat()).hasSize(1));
  }

  @Test
  void shouldExcludeWorkExperienceWithoutValidPositions() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience("Company", null),
                new CvResponse.WorkExperience("Company", List.of()),
                new CvResponse.WorkExperience(
                    "Company", List.of(position("Title", null, null, null)))));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludeEducationWithoutValidDegrees() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education("University", null),
                new CvResponse.Education("University", List.of()),
                new CvResponse.Education("University", List.of(degree(null, DATE, null, null)))));

    assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
  }

  @Test
  void shouldReturnEmptyResultsForNullOrEmptyLists() {
    var fromNull = map(new CvResponse(null, null, null));
    var fromEmpty = map(new CvResponse(List.of(), List.of(), List.of()));

    for (var tulos : List.of(fromNull, fromEmpty)) {
      assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
      assertThat(tulos.tyopaikat()).isEmpty();
      assertThat(tulos.teemat()).isEmpty();
    }
  }

  @Test
  void shouldKeepValidEducationAndExcludeInvalid() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education(null, List.of(degree("Degree", DATE, null, null))),
                new CvResponse.Education(
                    "Valid University", List.of(degree("Degree", DATE, null, null)))));

    assertThat(tulos.koulutuskokonaisuudet()).hasSize(1);
  }

  @Test
  void shouldExcludeEducationWithBlankInstitution() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education("  ", List.of(degree("Degree", DATE, null, null)))));

    assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
  }

  @Test
  void shouldExcludeDegreeWithNullName() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education(
                    "University",
                    List.of(degree(null, DATE, null, null), degree("Degree", DATE, null, null)))));

    assertThat(tulos.koulutuskokonaisuudet())
        .singleElement()
        .satisfies(it -> assertThat(it.koulutukset()).hasSize(1));
  }

  @Test
  void shouldExcludeWorkExperienceWithNullCompany() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(null, List.of(position("Title", DATE, null, null)))));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludePositionWithNullStartDate() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(
                    "Company",
                    List.of(
                        position("Title", null, null, null),
                        position("Other", DATE, null, null)))));

    assertThat(tulos.tyopaikat())
        .singleElement()
        .satisfies(it -> assertThat(it.toimenkuvat()).hasSize(1));
  }

  @Test
  void shouldExcludePositionWithInvalidInterval() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(
                    "Company",
                    List.of(
                        position(
                            "Title", LocalDate.of(2020, 6, 1), LocalDate.of(2020, 1, 1), null)))));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludeActivityWithNullName() {
    var tulos = map(withActivity(new CvResponse.Activity("Cat", null, "Desc", DATE, null)));

    assertThat(tulos.teemat()).isEmpty();
  }

  @Test
  void shouldExcludeActivityWithNullStartDate() {
    var tulos = map(withActivity(new CvResponse.Activity("Cat", "Name", "Desc", null, null)));

    assertThat(tulos.teemat()).isEmpty();
  }
}
