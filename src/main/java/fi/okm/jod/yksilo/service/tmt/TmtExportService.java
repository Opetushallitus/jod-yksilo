/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static net.logstash.logback.argument.StructuredArguments.value;

import fi.okm.jod.yksilo.config.tmt.TmtAuthorizationRepository.AccessToken;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.TapahtumaLoki;
import fi.okm.jod.yksilo.entity.TapahtumaLoki.Tapahtuma;
import fi.okm.jod.yksilo.entity.TapahtumaLoki.Tila;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.external.tmt.model.DescriptionItemExternal;
import fi.okm.jod.yksilo.external.tmt.model.EducationDtoExternal;
import fi.okm.jod.yksilo.external.tmt.model.EducationIntervalItemExternal;
import fi.okm.jod.yksilo.external.tmt.model.EmploymentDtoExternal;
import fi.okm.jod.yksilo.external.tmt.model.EscoEntityExternal;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternal;
import fi.okm.jod.yksilo.external.tmt.model.IntervalItemExternal;
import fi.okm.jod.yksilo.external.tmt.model.ProjectDtoExternal;
import fi.okm.jod.yksilo.repository.TapahtumaLokiRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceConflictException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
@Slf4j
public class TmtExportService {

  public static final String ALKAMASSA_TAI_JATKUU = "1";
  public static final String PAATTYNYT = "2";
  private final RestClient restClient;
  private final YksiloRepository yksiloRepository;
  private final TransactionTemplate transactionTemplate;
  private final TapahtumaLokiRepository tapahtumat;
  private final TmtConfiguration tmtConfiguration;

  TmtExportService(
      TapahtumaLokiRepository tapahtumat,
      YksiloRepository yksiloRepository,
      TmtConfiguration tmtConfiguration,
      RestClient tmtExportRestClient,
      PlatformTransactionManager txManager) {

    log.info("Creating TMT export service, API URL: {}", tmtConfiguration.getApiUrl());
    this.tapahtumat = tapahtumat;
    this.yksiloRepository = yksiloRepository;
    this.transactionTemplate = new TransactionTemplate(txManager);
    this.tmtConfiguration = tmtConfiguration;
    this.restClient = tmtExportRestClient;
  }

  public boolean canExport(JodUser jodUser) {
    return canExport(yksiloRepository.findById(jodUser.getId()).orElseThrow());
  }

  private boolean canExport(Yksilo yksilo) {
    return tmtConfiguration.isEnabled() && yksilo.getTervetuloapolku();
  }

  public void export(JodUser jodUser, AccessToken token) {
    record Result(TapahtumaLoki tapahtuma, FullProfileDtoExternal profile) {}

    Result result;
    try {
      result =
          transactionTemplate.execute(
              status -> {
                var yksilo = yksiloRepository.findById(jodUser.getId()).orElseThrow();
                if (!canExport(yksilo)) {
                  throw new ServiceException("Export not allowed");
                }
                var tapahtuma =
                    tapahtumat.saveAndFlush(
                        new TapahtumaLoki(yksilo, token.id(), Tapahtuma.TMT_VIENTI, Tila.KESKEN));
                return new Result(tapahtuma, toTmtProfile(yksilo));
              });
      if (result == null) {
        throw new ServiceException("Loading profile failed");
      }
    } catch (NoSuchElementException e) {
      throw new NotFoundException("User profile not found", e);
    } catch (DataIntegrityViolationException e) {
      throw new ServiceConflictException("Already exported", e);
    } catch (TransactionException e) {
      throw new ServiceException("Exporting profile failed", e);
    }

    var tapahtuma = result.tapahtuma();
    tapahtuma.setTila(Tila.VIRHE);
    try {
      restClient
          .put()
          .uri(tmtConfiguration.getApiUrl())
          .headers(
              headers -> {
                headers.add("KIPA-Subscription-Key", tmtConfiguration.getKipaSubscriptionKey());
                headers.setBearerAuth(token.token());
              })
          .contentType(MediaType.APPLICATION_JSON)
          .body(result.profile())
          .retrieve()
          .toBodilessEntity();
      tapahtuma.setTila(Tila.VALMIS);
      log.info("Successfully exported TMT profile for user {}", value("user", jodUser.getId()));
    } catch (HttpClientErrorException.BadRequest e) {
      log.error(
          "TMT export for user {} failed, invalid profile data: {}",
          value("user", jodUser.getId()),
          e.getMessage());
      throw new ServiceValidationException("TMT export failed: Invalid profile data", e);
    } catch (HttpClientErrorException.Forbidden e) {
      log.error(
          "TMT export for user {} failed, access denied: {}",
          value("user", jodUser.getId()),
          e.getMessage());
      throw new ServiceException("TMT export failed", e);
    } catch (HttpClientErrorException.Unauthorized e) {
      log.error(
          "TMT export for user {} failed, unauthorized (token expired?): {}",
          value("user", jodUser.getId()),
          e.getMessage());
      throw new ServiceException("TMT export failed", e);
    } catch (RestClientException e) {
      log.error(
          "TMT export for user {} failed: {}", value("user", jodUser.getId()), e.getMessage());
      throw new ServiceException("TMT export failed", e);
    } finally {
      tapahtumat.saveAndFlush(tapahtuma);
    }
  }

  static FullProfileDtoExternal toTmtProfile(Yksilo yksilo) {
    var profiili = new FullProfileDtoExternal();

    yksilo.getTyopaikat().stream()
        .flatMap(it -> it.getToimenkuvat().stream())
        .forEach(
            it -> {
              var item = new EmploymentDtoExternal();
              item.setEmployer(asStringMap(it.getTyopaikka().getNimi()));
              item.setEmployerNameHidden(false);
              item.setTitle(asStringMap(it.getNimi()));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new IntervalItemExternal()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .ongoing(it.getLoppuPvm() == null));
              }
              item.setDescription(mapDescriptionItem(it.getKuvaus(), it.getOsaamiset()));
              profiili.addEmploymentsItem(item);
            });

    yksilo.getKoulutusKokonaisuudet().stream()
        .flatMap(it -> it.getKoulutukset().stream())
        .forEach(
            it -> {
              var item = new EducationDtoExternal();
              item.setDegreeInstitution(asStringMap(it.getKokonaisuus().getNimi()));
              item.setCustomDegreeName(asStringMap(it.getNimi()));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new EducationIntervalItemExternal()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .statusCode(it.getLoppuPvm() == null ? ALKAMASSA_TAI_JATKUU : PAATTYNYT));
              }
              item.setDescription(mapDescriptionItem(it.getKuvaus(), it.getOsaamiset()));
              profiili.addEducationsItem(item);
            });

    yksilo.getToiminnot().stream()
        .flatMap(it -> it.getPatevyydet().stream())
        .forEach(
            it -> {
              var item = new ProjectDtoExternal();
              item.setTitle(asStringMap(it.getToiminto().getNimi()));
              if (it.getAlkuPvm() != null) {
                item.setInterval(
                    new IntervalItemExternal()
                        .startDate(it.getAlkuPvm())
                        .endDate(it.getLoppuPvm())
                        .ongoing(it.getLoppuPvm() == null));
              }
              item.setDescription(mapDescriptionItem(it.getNimi(), it.getOsaamiset()));
              profiili.addProjectsItem(item);
            });

    return profiili;
  }

  static DescriptionItemExternal mapDescriptionItem(
      LocalizedString kuvaus, Collection<YksilonOsaaminen> osaamiset) {
    if (kuvaus != null || !osaamiset.isEmpty()) {
      var item = new DescriptionItemExternal();
      item.setDescription(asStringMap(kuvaus));
      osaamiset.stream()
          .limit(120)
          .forEach(
              o ->
                  item.addSkillsItem(
                      new EscoEntityExternal().uri(o.getOsaaminen().getUri().toString())));
      return item;
    }
    return null;
  }

  static Map<String, String> asStringMap(LocalizedString ls) {
    return ls == null
        ? null
        : ls.asMap().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Entry::getValue));
  }
}
