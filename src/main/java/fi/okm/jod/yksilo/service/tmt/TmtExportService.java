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
import static fi.okm.jod.yksilo.service.tmt.TmtApiConstants.KOUTULUS_TILA_PAATTYNYT;
import static fi.okm.jod.yksilo.service.tmt.TmtApiConstants.PROFILE_ITEM_LIMIT;
import static fi.okm.jod.yksilo.service.tmt.TmtApiConstants.SKILL_LIMIT;

import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.TmtExportDto;
import fi.okm.jod.yksilo.dto.profiili.TmtExportDto.Syy;
import fi.okm.jod.yksilo.dto.profiili.TmtExportDto.Tulos;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.external.tmt.model.DescriptionItemExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.EducationDtoExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.EducationIntervalItemExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.EmploymentDtoExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.EscoValueExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.IntervalItemExternalPut;
import fi.okm.jod.yksilo.external.tmt.model.ProjectDtoExternalPut;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import io.micrometer.tracing.annotation.NewSpan;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
@Slf4j
public class TmtExportService {

  private final RestClient restClient;
  private final YksiloRepository yksiloRepository;
  private final TransactionTemplate transactionTemplate;
  private final TmtConfiguration tmtConfiguration;

  TmtExportService(
      YksiloRepository yksiloRepository,
      TmtConfiguration tmtConfiguration,
      RestClient tmtRestClient,
      PlatformTransactionManager txManager) {

    log.info(
        "Creating TMT export service, API URL: {}", tmtConfiguration.getExportApi().getApiUrl());
    this.yksiloRepository = yksiloRepository;
    this.transactionTemplate = new TransactionTemplate(txManager);
    this.tmtConfiguration = tmtConfiguration;
    this.restClient = tmtRestClient;
  }

  public boolean canExport(JodUser jodUser) {
    return canExport(yksiloRepository.findById(jodUser.getId()).orElseThrow());
  }

  private boolean canExport(Yksilo yksilo) {
    return tmtConfiguration.isEnabled() && yksilo.getTervetuloapolku();
  }

  @NewSpan("tmt.export-profile")
  public TmtExportDto export(JodUser jodUser, OAuth2AccessToken token) {

    if (token == null
        || (token.getExpiresAt() instanceof Instant instant && instant.isBefore(Instant.now()))) {
      throw new ServiceValidationException("TMT export failed: Access token is missing or expired");
    }

    TmtProfileResult result;
    try {
      result =
          transactionTemplate.execute(
              status -> {
                var yksilo = yksiloRepository.findById(jodUser.getId()).orElseThrow();
                if (!canExport(yksilo)) {
                  throw new ServiceException("TMT export not allowed");
                }
                return toTmtProfile(yksilo);
              });
      if (result == null) {
        throw new ServiceException("Loading profile failed");
      }
    } catch (NoSuchElementException e) {
      throw new NotFoundException("User profile not found", e);
    } catch (TransactionException e) {
      throw new ServiceException("Exporting profile failed", e);
    }

    try {
      restClient
          .put()
          .uri(tmtConfiguration.getExportApi().getApiUrl())
          .headers(
              headers -> {
                headers.add(
                    "KIPA-Subscription-Key",
                    tmtConfiguration.getExportApi().getKipaSubscriptionKey());
                headers.setBearerAuth(token.getTokenValue());
              })
          .contentType(MediaType.APPLICATION_JSON)
          .body(result.profile())
          .retrieve()
          .toBodilessEntity();
      log.atInfo().addMarker(LogMarker.AUDIT).log("Successfully exported TMT profile");
    } catch (HttpClientErrorException.BadRequest e) {
      log.atWarn().log("TMT export failed, invalid profile data: {}", e.getMessage());
      throw new ServiceException("TMT export failed: Invalid profile data", e);
    } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log(
              "TMT export failed, {}: {}, responseHeaders={}",
              e.getStatusCode(),
              e.getMessage(),
              e.getResponseHeaders());
      throw new ServiceException("TMT export failed", e);
    } catch (Exception e) {
      log.atWarn().log("TMT export failed: {}", e.getMessage());
      throw new ServiceException("TMT export failed", e);
    }

    var warnings = result.warnings();
    return new TmtExportDto(warnings.isEmpty() ? Tulos.OK : Tulos.VIETY_OSITTAIN, warnings);
  }

  record TmtProfileResult(FullProfileDtoExternalPut profile, Set<Syy> warnings) {}

  static TmtProfileResult toTmtProfile(Yksilo yksilo) {
    var profile = new FullProfileDtoExternalPut();

    yksilo.getTyopaikat().stream()
        .flatMap(it -> it.getToimenkuvat().stream())
        .limit(PROFILE_ITEM_LIMIT)
        .forEach(
            it -> {
              var item = new EmploymentDtoExternalPut();
              item.setEmployer(truncateValues(asStringMap(it.getTyopaikka().getNimi()), 254));
              item.setEmployerNameHidden(false);
              item.setTitle(truncateValues(asStringMap(it.getNimi()), 128));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new IntervalItemExternalPut()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .ongoing(it.getLoppuPvm() == null));
              }
              item.setDescription(mapDescriptionItem(it.getKuvaus(), it.getOsaamiset()));
              profile.addEmploymentsItem(item);
            });

    yksilo.getKoulutusKokonaisuudet().stream()
        .flatMap(it -> it.getKoulutukset().stream())
        .limit(PROFILE_ITEM_LIMIT)
        .forEach(
            it -> {
              var item = new EducationDtoExternalPut();
              item.setDegreeInstitution(
                  truncateValues(asStringMap(it.getKokonaisuus().getNimi()), 128));
              item.setCustomDegreeName(truncateValues(asStringMap(it.getNimi()), 128));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new EducationIntervalItemExternalPut()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .statusCode(
                            it.getLoppuPvm() == null
                                ? KOULUTUS_TILA_ALKAMASSA_TAI_JATKUU
                                : KOUTULUS_TILA_PAATTYNYT));
              }
              item.setDescription(mapDescriptionItem(it.getKuvaus(), it.getOsaamiset()));
              profile.addEducationsItem(item);
            });

    yksilo.getTeemat().stream()
        .flatMap(it -> it.getToiminnot().stream())
        .limit(PROFILE_ITEM_LIMIT)
        .forEach(
            it -> {
              var item = new ProjectDtoExternalPut();
              item.setTitle(truncateValues(asStringMap(it.getNimi()), 254));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new IntervalItemExternalPut()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .ongoing(it.getLoppuPvm() == null));
              }
              item.setDescription(mapDescriptionItem(it.getKuvaus(), it.getOsaamiset()));
              profile.addProjectsItem(item);
            });

    var warnings = EnumSet.noneOf(Syy.class);
    boolean skillLimitExceeded =
        yksilo.getTyopaikat().stream()
                .flatMap(it -> it.getToimenkuvat().stream())
                .limit(PROFILE_ITEM_LIMIT)
                .anyMatch(it -> it.getOsaamiset().size() > SKILL_LIMIT)
            || yksilo.getKoulutusKokonaisuudet().stream()
                .flatMap(it -> it.getKoulutukset().stream())
                .limit(PROFILE_ITEM_LIMIT)
                .anyMatch(it -> it.getOsaamiset().size() > SKILL_LIMIT)
            || yksilo.getTeemat().stream()
                .flatMap(it -> it.getToiminnot().stream())
                .limit(PROFILE_ITEM_LIMIT)
                .anyMatch(it -> it.getOsaamiset().size() > SKILL_LIMIT);
    if (skillLimitExceeded) {
      warnings.add(Syy.LIIKAA_OSAAMISIA);
    }

    return new TmtProfileResult(profile, warnings);
  }

  static DescriptionItemExternalPut mapDescriptionItem(
      LocalizedString kuvaus, Collection<YksilonOsaaminen> osaamiset) {
    if (kuvaus != null || !osaamiset.isEmpty()) {
      var item = new DescriptionItemExternalPut();
      item.setDescription(truncateValues(asStringMap(kuvaus), 5000));
      osaamiset.stream()
          .limit(SKILL_LIMIT)
          .forEach(
              o ->
                  item.addSkillsItem(
                      new EscoValueExternalPut().uri(o.getOsaaminen().getUri().toString())));
      return item;
    }
    return null;
  }

  static Map<String, String> truncateValues(Map<String, String> map, int maxLength) {
    if (map == null) {
      return null;
    }
    return map.entrySet().stream()
        .collect(
            HashMap::new,
            (m, e) -> m.put(e.getKey(), truncate(e.getValue(), maxLength)),
            HashMap::putAll);
  }

  static String truncate(String s, int maxLength) {
    return s != null && s.length() > maxLength ? s.substring(0, maxLength) : s;
  }

  static Map<String, String> asStringMap(LocalizedString ls) {
    if (ls == null || ls.asMap().isEmpty()) {
      return null;
    }

    var map =
        ls.asMap().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Entry::getValue));

    // Fill in missing default language (TMT profile UI expects that at least "fi" is present)
    map.computeIfAbsent(
        Kieli.FI.getKoodi(),
        k -> map.getOrDefault(Kieli.SV.getKoodi(), map.get(Kieli.EN.getKoodi())));

    return map;
  }
}
