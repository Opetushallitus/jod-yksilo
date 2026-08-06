/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import static java.util.stream.Collectors.toCollection;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.TeemaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Maps a {@link CvResponse} to {@link CvTehtavaDto.Tulos}. */
@Component
@RequiredArgsConstructor
@Slf4j
class CvResponseMapper {

  private final Validator validator;

  CvTehtavaDto.Tulos toTulos(CvResponse response, Kieli kieli) {
    var violations = new HashSet<ConstraintViolation<?>>();

    var tulos =
        new CvTehtavaDto.Tulos(
            mapEducations(response.education(), kieli, violations),
            mapWorkExperiences(response.workExperience(), kieli, violations),
            mapActivities(response.otherActivities(), kieli, violations));

    if (!violations.isEmpty()) {
      log.atWarn()
          .addKeyValue(
              "validationErrors",
              violations.stream()
                  .map(cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage())
                  .toList())
          .log("CV extraction had validation errors, invalid items ignored");
    }

    return tulos;
  }

  private <T> boolean isValid(T dto, Set<ConstraintViolation<?>> violations) {
    var result = validator.validate(dto);
    violations.addAll(result);
    return result.isEmpty();
  }

  /**
   * Maps the items of a group (positions of an employer, degrees of an institution), dropping the
   * invalid ones. A group left without any valid items fails validation and is dropped as a whole.
   */
  private <T, R> Set<R> mapValid(
      List<T> items, Function<T, R> mapper, Set<ConstraintViolation<?>> violations) {
    if (items == null) {
      return Set.of();
    }
    return items.stream()
        .map(mapper)
        .filter(it -> isValid(it, violations))
        .collect(toCollection(LinkedHashSet::new));
  }

  private List<KoulutusKokonaisuusDto> mapEducations(
      List<CvResponse.Education> educations, Kieli kieli, Set<ConstraintViolation<?>> violations) {
    if (educations == null) {
      return List.of();
    }
    return educations.stream()
        .map(e -> mapEducation(e, kieli, violations))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private KoulutusKokonaisuusDto mapEducation(
      CvResponse.Education education, Kieli kieli, Set<ConstraintViolation<?>> violations) {
    return new KoulutusKokonaisuusDto(
        UUID.randomUUID(),
        localizedString(education.institution(), kieli),
        TuontiLahde.CV_TUONTI,
        mapValid(education.allDegrees(), degree -> mapKoulutus(degree, kieli), violations));
  }

  private KoulutusDto mapKoulutus(CvResponse.Degree degree, Kieli kieli) {
    return KoulutusDto.builder()
        .id(UUID.randomUUID())
        .nimi(localizedString(degree.degree(), kieli))
        .kuvaus(localizedString(degree.details(), kieli))
        .alkuPvm(degree.startDate())
        .loppuPvm(degree.endDate())
        .build();
  }

  private List<TyopaikkaDto> mapWorkExperiences(
      List<CvResponse.WorkExperience> workExperiences,
      Kieli kieli,
      Set<ConstraintViolation<?>> violations) {
    if (workExperiences == null) {
      return List.of();
    }
    return workExperiences.stream()
        .map(e -> mapWorkExperience(e, kieli, violations))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private TyopaikkaDto mapWorkExperience(
      CvResponse.WorkExperience workExperience,
      Kieli kieli,
      Set<ConstraintViolation<?>> violations) {
    return new TyopaikkaDto(
        UUID.randomUUID(),
        localizedString(workExperience.company(), kieli),
        TuontiLahde.CV_TUONTI,
        mapValid(
            workExperience.allPositions(), position -> mapToimenkuva(position, kieli), violations));
  }

  private ToimenkuvaDto mapToimenkuva(CvResponse.Position position, Kieli kieli) {
    return new ToimenkuvaDto(
        UUID.randomUUID(),
        localizedString(position.title(), kieli),
        localizedString(position.description(), kieli),
        position.startDate(),
        position.endDate(),
        null);
  }

  private List<TeemaDto> mapActivities(
      List<CvResponse.Activity> activities, Kieli kieli, Set<ConstraintViolation<?>> violations) {
    if (activities == null) {
      return List.of();
    }
    return activities.stream()
        .map(e -> mapActivity(e, kieli))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private TeemaDto mapActivity(CvResponse.Activity activity, Kieli kieli) {
    var nimi = localizedString(activity.name(), kieli);
    var toiminto =
        new ToimintoDto(
            UUID.randomUUID(),
            nimi,
            localizedString(activity.description(), kieli),
            activity.startDate(),
            activity.endDate(),
            null);
    return new TeemaDto(UUID.randomUUID(), nimi, TuontiLahde.CV_TUONTI, Set.of(toiminto));
  }

  private static LocalizedString localizedString(String value, Kieli kieli) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new LocalizedString(Map.of(kieli, value));
  }
}
