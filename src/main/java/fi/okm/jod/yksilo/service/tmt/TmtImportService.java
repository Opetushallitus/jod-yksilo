/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.TmtImportDto;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.external.tmt.model.DescriptionItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EducationDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EducationIntervalItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.EmploymentDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.IntervalItemExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.ProjectDtoExternalGet;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.ServiceException;
import jakarta.validation.Validator;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
@Slf4j
public class TmtImportService {

  public static final String ALKAMASSA_TAI_JATKUU = "1";
  private final RestClient restClient;
  private final YksiloRepository yksilot;
  private final TmtConfiguration tmtConfiguration;
  private final Validator validator;
  private final OsaaminenService osaamiset;

  TmtImportService(
      YksiloRepository yksilot,
      TmtConfiguration tmtConfiguration,
      RestClient tmtRestClient,
      OsaaminenService osaamiset,
      Validator validator) {

    log.info(
        "Creating TMT import service, API URL: {}", tmtConfiguration.getImportApi().getApiUrl());
    this.yksilot = yksilot;
    this.tmtConfiguration = tmtConfiguration;
    this.restClient = tmtRestClient;
    this.osaamiset = osaamiset;
    this.validator = validator;
  }

  public TmtImportDto importProfile(JodUser jodUser, OAuth2AccessToken token) {

    if (token == null
        || (token.getExpiresAt() instanceof Instant instant && instant.isBefore(Instant.now()))) {
      throw new ServiceException("TMT import failed: Access token is missing or expired");
    }
    yksilot.findById(jodUser.getId()).orElseThrow(() -> new ServiceException("User not found"));
    log.atInfo().addMarker(LogMarker.AUDIT).log("Importing TMT profile");

    try {
      var api = tmtConfiguration.getImportApi();
      var result =
          fromTmtProfile(
              restClient
                  .get()
                  .uri(api.getApiUrl())
                  .headers(
                      headers -> {
                        headers.add("KIPA-Subscription-Key", api.getKipaSubscriptionKey());
                        headers.setBearerAuth(token.getTokenValue());
                      })
                  .retrieve()
                  .body(FullProfileDtoExternalGet.class));

      var violations = validator.validate(result, Add.class);
      if (violations.isEmpty()) {
        log.atInfo().addMarker(LogMarker.AUDIT).log("TMT import successful");
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
    } catch (HttpClientErrorException.BadRequest e) {
      log.atWarn().log("TMT import failed, invalid request {}", e.getMessage());
      throw new TmtImportException("TMT import failed", e);
    } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log("TMT import failed, {}: {}", e.getStatusCode(), e.getMessage());
      throw new TmtImportException("TMT import failed", e);
    } catch (TmtImportException e) {
      throw e;
    } catch (Exception e) {
      log.atWarn().log("TMT import failed: {}", e.getMessage());
      throw new ServiceException("TMT import failed", e);
    }
  }

  public TmtImportDto fromTmtProfile(FullProfileDtoExternalGet profile) {
    if (profile == null) {
      return new TmtImportDto(Set.of(), Set.of(), Set.of());
    }

    var tyopaikat =
        profile.getEmployments() == null
            ? Set.<TyopaikkaDto>of()
            : profile.getEmployments().stream()
                .map(
                    employment -> {
                      var employerName = asLocalizedString(employment.getEmployer());
                      var toimenkuva = mapToToimenkuva(employment);
                      return new TyopaikkaDto(null, employerName, Set.of(toimenkuva));
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
            : profile.getProjects().stream().map(this::mapToToiminto).collect(Collectors.toSet());

    return new TmtImportDto(tyopaikat, koulutuskokonaisuudet, toiminnot);
  }

  ToimenkuvaDto mapToToimenkuva(EmploymentDtoExternalGet employment) {
    var nimi = asLocalizedString(employment.getTitle());
    var kuvaus = extractDescription(employment.getDescription());
    var osaamiset = extractSkills(employment.getDescription());
    var alkuPvm = extractStartDate(employment.getInterval());
    var loppuPvm = extractEndDate(employment.getInterval());

    return new ToimenkuvaDto(null, nimi, kuvaus, alkuPvm, loppuPvm, osaamiset);
  }

  KoulutusDto mapToKoulutus(EducationDtoExternalGet education) {

    var nimi = asLocalizedString(education.getCustomDegreeName());
    if (nimi == null && education.getDegreeCode() instanceof String degreeCode) {
      // TODO: map degree codes to localized names
      nimi =
          new LocalizedString(
              Map.of(
                  Kieli.FI,
                  "koulutuskoodi: " + degreeCode,
                  Kieli.SV,
                  "utbildningskod: " + degreeCode,
                  Kieli.EN,
                  "degree code: " + degreeCode));
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

  ToimintoDto mapToToiminto(ProjectDtoExternalGet project) {
    var toimintoNimi = asLocalizedString(project.getTitle());
    var patevyysNimi = extractDescription(project.getDescription());
    var osaamiset = extractSkills(project.getDescription());
    var alkuPvm = extractStartDate(project.getInterval());
    var loppuPvm = extractEndDate(project.getInterval());

    var patevyys = new PatevyysDto(null, patevyysNimi, null, alkuPvm, loppuPvm, osaamiset);
    return new ToimintoDto(null, toimintoNimi, Set.of(patevyys));
  }

  LocalizedString extractDescription(DescriptionItemExternalGet description) {
    if (description != null && description.getDescription() != null) {
      return asLocalizedString(description.getDescription());
    }
    return null;
  }

  Set<URI> extractSkills(DescriptionItemExternalGet description) {
    if (description != null && description.getSkills() != null) {
      var uris = osaamiset.getAll().keySet();
      return description.getSkills().stream()
          .filter(it -> it.getUri() != null)
          .map(it -> URI.create(it.getUri()))
          .filter(uris::contains)
          .collect(Collectors.toSet());
    }
    return null;
  }

  static LocalDate extractStartDate(IntervalItemExternalGet interval) {
    return interval != null ? interval.getStartDate() : null;
  }

  static LocalDate extractEndDate(IntervalItemExternalGet interval) {
    if (interval != null) {
      return Boolean.TRUE.equals(interval.getOngoing()) ? null : interval.getEndDate();
    }
    return null;
  }

  static LocalDate extractEducationStartDate(EducationIntervalItemExternalGet interval) {
    return interval != null ? interval.getStartDate() : null;
  }

  static LocalDate extractEducationEndDate(EducationIntervalItemExternalGet interval) {
    if (interval != null) {
      if (ALKAMASSA_TAI_JATKUU.equals(interval.getStatusCode())) {
        return null;
      }
      return interval.getEndDate();
    }
    return null;
  }

  private static final Map<String, Kieli> languageCodes =
      Arrays.stream(Kieli.values())
          .collect(Collectors.toUnmodifiableMap(Kieli::toString, Function.identity()));

  // Converts a map (lang, string) to LocalizedString, ignoring unsupported languages
  static LocalizedString asLocalizedString(Object str) {
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
