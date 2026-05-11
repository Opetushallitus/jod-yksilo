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

  @Test
  void shouldMapValidResponse() {
    var response =
        new CvResponse(
            List.of(
                new CvResponse.WorkExperience("Company", "Title", DATE, null, null, "Desc", null)),
            List.of(new CvResponse.Education("University", "Degree", DATE, null, "Details")),
            List.of(new CvResponse.Activity("Hobby", "Name", "Description", DATE, null)));

    var tulos = map(response);

    assertThat(tulos.koulutuskokonaisuudet()).hasSize(1);
    assertThat(tulos.tyopaikat()).hasSize(1);
    assertThat(tulos.toiminnot()).hasSize(1);
  }

  @Test
  void shouldReturnEmptyResultsForNullOrEmptyLists() {
    var fromNull = map(new CvResponse(null, null, null));
    var fromEmpty = map(new CvResponse(List.of(), List.of(), List.of()));

    for (var tulos : List.of(fromNull, fromEmpty)) {
      assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
      assertThat(tulos.tyopaikat()).isEmpty();
      assertThat(tulos.toiminnot()).isEmpty();
    }
  }

  @Test
  void shouldKeepValidEducationAndExcludeInvalid() {
    var tulos =
        map(
            withEducation(
                new CvResponse.Education(null, "Degree", DATE, null, null),
                new CvResponse.Education("Valid University", "Degree", DATE, null, null)));

    assertThat(tulos.koulutuskokonaisuudet()).hasSize(1);
  }

  @Test
  void shouldExcludeEducationWithBlankInstitution() {
    var tulos = map(withEducation(new CvResponse.Education("  ", "Degree", DATE, null, null)));

    assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
  }

  @Test
  void shouldExcludeEducationWithNullDegree() {
    var tulos = map(withEducation(new CvResponse.Education("University", null, DATE, null, null)));

    assertThat(tulos.koulutuskokonaisuudet()).isEmpty();
  }

  @Test
  void shouldExcludeWorkExperienceWithNullCompany() {
    var tulos =
        map(withWork(new CvResponse.WorkExperience(null, "Title", DATE, null, null, null, null)));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludeWorkExperienceWithNullStartDate() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience("Company", "Title", null, null, null, null, null)));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludeWorkExperienceWithInvalidInterval() {
    var tulos =
        map(
            withWork(
                new CvResponse.WorkExperience(
                    "Company",
                    "Title",
                    LocalDate.of(2020, 6, 1),
                    LocalDate.of(2020, 1, 1),
                    null,
                    null,
                    null)));

    assertThat(tulos.tyopaikat()).isEmpty();
  }

  @Test
  void shouldExcludeActivityWithNullName() {
    var tulos = map(withActivity(new CvResponse.Activity("Cat", null, "Desc", DATE, null)));

    assertThat(tulos.toiminnot()).isEmpty();
  }

  @Test
  void shouldExcludeActivityWithNullStartDate() {
    var tulos = map(withActivity(new CvResponse.Activity("Cat", "Name", "Desc", null, null)));

    assertThat(tulos.toiminnot()).isEmpty();
  }
}
