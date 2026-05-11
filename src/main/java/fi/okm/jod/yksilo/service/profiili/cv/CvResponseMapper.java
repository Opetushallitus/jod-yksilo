/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Maps a flattened {@link CvResponse} to {@link CvTehtavaDto.Tulos}. */
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

  private List<KoulutusKokonaisuusDto> mapEducations(
      List<CvResponse.Education> educations, Kieli kieli, Set<ConstraintViolation<?>> violations) {
    if (educations == null) {
      return List.of();
    }
    return educations.stream()
        .map(e -> mapEducation(e, kieli))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private KoulutusKokonaisuusDto mapEducation(CvResponse.Education education, Kieli kieli) {
    var nimi = localizedString(education.institution(), kieli);
    var koulutus =
        KoulutusDto.builder()
            .id(UUID.randomUUID())
            .nimi(localizedString(education.degree(), kieli))
            .kuvaus(localizedString(education.details(), kieli))
            .alkuPvm(education.startDate())
            .loppuPvm(education.endDate())
            .build();
    return new KoulutusKokonaisuusDto(UUID.randomUUID(), nimi, Set.of(koulutus));
  }

  private List<TyopaikkaDto> mapWorkExperiences(
      List<CvResponse.WorkExperience> workExperiences,
      Kieli kieli,
      Set<ConstraintViolation<?>> violations) {
    if (workExperiences == null) {
      return List.of();
    }
    return workExperiences.stream()
        .map(e -> mapWorkExperience(e, kieli))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private TyopaikkaDto mapWorkExperience(CvResponse.WorkExperience workExperience, Kieli kieli) {
    var nimi = localizedString(workExperience.company(), kieli);
    var toimenkuva =
        new ToimenkuvaDto(
            UUID.randomUUID(),
            localizedString(workExperience.title(), kieli),
            localizedString(workExperience.description(), kieli),
            workExperience.startDate(),
            workExperience.endDate(),
            null);
    return new TyopaikkaDto(UUID.randomUUID(), nimi, Set.of(toimenkuva));
  }

  private List<ToimintoDto> mapActivities(
      List<CvResponse.Activity> activities, Kieli kieli, Set<ConstraintViolation<?>> violations) {
    if (activities == null) {
      return List.of();
    }
    return activities.stream()
        .map(e -> mapActivity(e, kieli))
        .filter(it -> isValid(it, violations))
        .toList();
  }

  private ToimintoDto mapActivity(CvResponse.Activity activity, Kieli kieli) {
    var nimi = localizedString(activity.name(), kieli);
    var patevyys =
        new PatevyysDto(
            UUID.randomUUID(),
            nimi,
            localizedString(activity.description(), kieli),
            activity.startDate(),
            activity.endDate(),
            null);
    return new ToimintoDto(UUID.randomUUID(), nimi, Set.of(patevyys));
  }

  private static LocalizedString localizedString(String value, Kieli kieli) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new LocalizedString(Map.of(kieli, value));
  }
}
