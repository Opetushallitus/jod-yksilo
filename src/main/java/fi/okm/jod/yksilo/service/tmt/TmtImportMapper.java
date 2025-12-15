/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static fi.okm.jod.yksilo.service.tmt.TmtApiConstants.KOULUTUS_TILA_ALKAMASSA_TAI_JATKUU;
import static fi.okm.jod.yksilo.service.tmt.TmtApiConstants.KOULUTUS_TILA_KESKEYTYNYT;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.entity.koodisto.Koulutuskoodi;
import fi.okm.jod.yksilo.external.tmt.model.DescriptionItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EducationDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EducationIntervalItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EmploymentDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.IntervalItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.ProjectDtoExternalGet;
import fi.okm.jod.yksilo.repository.koodisto.KoulutuskoodiRepository;
import fi.okm.jod.yksilo.service.OsaaminenService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class TmtImportMapper {

  private final Validator validator;
  private final OsaaminenService osaaminenService;
  private final KoulutuskoodiRepository koulutuskoodit;

  record ImportDto(
      Set<@Valid TyopaikkaDto> tyopaikat,
      Set<@Valid KoulutusKokonaisuusDto> koulutuskokonaisuudet,
      Set<@Valid ToimintoDto> toiminnot) {}

  ImportDto map(FullProfileDtoExternalGet tmtProfile) {

    var result = fromTmtProfile(tmtProfile);
    var violations = validator.validate(result, Add.class);
    if (violations.isEmpty()) {
      return result;
    } else {
      log.atWarn()
          .addKeyValue(
              "validationErrors",
              violations.stream()
                  .map(cv -> cv == null ? "null" : cv.getPropertyPath() + ": " + cv.getMessage())
                  .toList())
          .log("TMT import failed due to validation errors");
      throw new TmtImportException("TMT import failed: invalid response");
    }
  }

  private ImportDto fromTmtProfile(FullProfileDtoExternalGet profile) {
    if (profile == null) {
      return new ImportDto(Set.of(), Set.of(), Set.of());
    }

    var tyopaikat =
        profile.getEmployments() == null
            ? Set.<TyopaikkaDto>of()
            : profile.getEmployments().stream()
                .flatMap(
                    employment -> {
                      var employerName = asLocalizedString(employment.getEmployer());
                      return mapToToimenkuva(employment)
                          .map(
                              toimenkuva ->
                                  new TyopaikkaDto(null, employerName, Set.of(toimenkuva)))
                          .stream();
                    })
                .collect(Collectors.toSet());

    var koulutuskokonaisuudet =
        profile.getEducations() == null
            ? Set.<KoulutusKokonaisuusDto>of()
            : profile.getEducations().stream()
                .map(
                    education -> {
                      var institutionName = asLocalizedString(education.getDegreeInstitution());
                      var koulutus = mapToKoulutus(education);
                      return new KoulutusKokonaisuusDto(null, institutionName, Set.of(koulutus));
                    })
                .collect(Collectors.toSet());

    var toiminnot =
        profile.getProjects() == null
            ? Set.<ToimintoDto>of()
            : profile.getProjects().stream()
                .flatMap(it -> mapToToiminto(it).stream())
                .collect(Collectors.toSet());

    return new ImportDto(tyopaikat, koulutuskokonaisuudet, toiminnot);
  }

  private Optional<ToimenkuvaDto> mapToToimenkuva(EmploymentDtoExternalGet employment) {
    var nimi = asLocalizedString(employment.getTitle());
    var kuvaus = extractDescription(employment.getDescription());
    var osaamiset = extractSkills(employment.getDescription());
    var alkuPvm = extractStartDate(employment.getInterval());
    var loppuPvm = extractEndDate(employment.getInterval());

    if (alkuPvm == null) {
      log.debug("Ignoring a Toimenkuva with missing start date");
      return Optional.empty();
    }
    return Optional.of(new ToimenkuvaDto(null, nimi, kuvaus, alkuPvm, loppuPvm, osaamiset));
  }

  private KoulutusDto mapToKoulutus(EducationDtoExternalGet education) {

    var nimi = asLocalizedString(education.getCustomDegreeName());

    if (nimi == null && education.getDegreeCode() instanceof String degreeCode) {
      nimi = koulutuskoodit.findByKoodi(degreeCode).map(Koulutuskoodi::getNimi).orElse(null);
    }

    var kuvaus = extractDescription(education.getDescription());
    var osaamiset = extractSkills(education.getDescription());
    var alkuPvm = extractEducationStartDate(education.getInterval());
    var loppuPvm = extractEducationEndDate(education.getInterval());

    return KoulutusDto.builder()
        .id(null)
        .nimi(nimi)
        .kuvaus(kuvaus)
        .alkuPvm(alkuPvm)
        .loppuPvm(loppuPvm)
        .osaamiset(osaamiset)
        .osaamisetOdottaaTunnistusta(false)
        .osaamisetTunnistusEpaonnistui(false)
        .osasuoritukset(null)
        .build();
  }

  private Optional<ToimintoDto> mapToToiminto(ProjectDtoExternalGet project) {
    var nimi = asLocalizedString(project.getTitle());
    var kuvaus = extractDescription(project.getDescription());
    var osaamiset = extractSkills(project.getDescription());
    var alkuPvm = extractStartDate(project.getInterval());
    var loppuPvm = extractEndDate(project.getInterval());

    if (alkuPvm == null) {
      log.debug("Ignoring a Toiminto with missing start date");
      return Optional.empty();
    }
    var patevyys = new PatevyysDto(null, nimi, kuvaus, alkuPvm, loppuPvm, osaamiset);
    return Optional.of(new ToimintoDto(null, nimi, Set.of(patevyys)));
  }

  private LocalizedString extractDescription(DescriptionItemExternalGet description) {
    if (description != null && description.getDescription() != null) {
      return asLocalizedString(description.getDescription());
    }
    return null;
  }

  private Set<URI> extractSkills(DescriptionItemExternalGet description) {
    if (description != null && description.getSkills() != null) {
      var uris = osaaminenService.getAll().keySet();
      return description.getSkills().stream()
          .filter(it -> it.getUri() != null)
          .map(it -> URI.create(it.getUri()))
          .filter(
              it -> {
                if (!uris.contains(it)) {
                  log.debug("Ignoring unknown osaaminen URI from TMT: {}", it);
                  return false;
                }
                return true;
              })
          .collect(Collectors.toSet());
    }
    return Set.of();
  }

  private static LocalDate extractStartDate(IntervalItemExternalGet interval) {
    return interval == null ? null : interval.getStartDate();
  }

  private static LocalDate extractEndDate(IntervalItemExternalGet interval) {
    if (interval != null) {
      return Boolean.TRUE.equals(interval.getOngoing()) ? null : interval.getEndDate();
    }
    return null;
  }

  private static LocalDate extractEducationStartDate(EducationIntervalItemExternalGet interval) {
    return interval == null ? null : interval.getStartDate();
  }

  private static LocalDate extractEducationEndDate(EducationIntervalItemExternalGet interval) {
    if (interval != null && interval.getStatusCode() != null) {
      return switch (interval.getStatusCode()) {
        case KOULUTUS_TILA_ALKAMASSA_TAI_JATKUU -> null;
        case KOULUTUS_TILA_KESKEYTYNYT -> interval.getAbortedDate();
        default -> interval.getEndDate();
      };
    }
    return null;
  }

  private static final Map<String, Kieli> languageCodes =
      Arrays.stream(Kieli.values())
          .collect(Collectors.toUnmodifiableMap(Kieli::toString, Function.identity()));

  // Converts a map (lang, string) to LocalizedString, ignoring unsupported languages
  private static LocalizedString asLocalizedString(Object str) {
    if (str == null) {
      return null;
    }
    if (str instanceof Map<?, ?> map) {
      return LocalizedString.fromJsonNormalized(
          map.entrySet().stream()
              .filter(
                  e ->
                      e.getKey() instanceof String key
                          && e.getValue() instanceof String
                          && languageCodes.containsKey(key))
              .collect(
                  Collectors.toMap(
                      e -> languageCodes.get(e.getKey()), e -> e.getValue().toString())));
    }
    throw new TmtImportException("Unexpected type for a localized string");
  }
}
