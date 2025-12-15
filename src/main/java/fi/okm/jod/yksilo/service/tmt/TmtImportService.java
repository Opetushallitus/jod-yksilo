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
import fi.okm.jod.yksilo.dto.profiili.TmtImportDto;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import fi.okm.jod.yksilo.service.profiili.TyopaikkaService;
import fi.okm.jod.yksilo.service.tmt.TmtImportMapper.ImportDto;
import jakarta.transaction.Transactional;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
@Slf4j
public class TmtImportService {

  private final RestClient restClient;
  private final YksiloRepository yksilot;
  private final TmtConfiguration tmtConfiguration;
  private final ProfilePersister persister;

  TmtImportService(
      YksiloRepository yksilot,
      TmtConfiguration tmtConfiguration,
      RestClient tmtRestClient,
      ProfilePersister persister) {

    log.info(
        "Creating TMT import service, API URL: {}", tmtConfiguration.getImportApi().getApiUrl());
    this.yksilot = yksilot;
    this.tmtConfiguration = tmtConfiguration;
    this.restClient = tmtRestClient;
    this.persister = persister;
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
          restClient
              .get()
              .uri(api.getApiUrl())
              .headers(
                  headers -> {
                    headers.add("KIPA-Subscription-Key", api.getKipaSubscriptionKey());
                    headers.setBearerAuth(token.getTokenValue());
                  })
              .retrieve()
              .body(FullProfileDtoExternalGet.class);
      var dto = persister.save(jodUser, result);
      log.atInfo().addMarker(LogMarker.AUDIT).log("TMT profile imported successfully");
      return dto;
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

  @Component
  @RequiredArgsConstructor
  static class ProfilePersister {
    private final KoulutusKokonaisuusService koulutukset;
    private final TyopaikkaService tyopaikkat;
    private final ToimintoService toiminnot;
    private final TmtImportMapper mapper;

    @Transactional
    TmtImportDto save(JodUser user, FullProfileDtoExternalGet profile) {
      ImportDto importDto = mapper.map(profile);
      return new TmtImportDto(
          tyopaikkat.add(user, importDto.tyopaikat()),
          koulutukset.add(user, importDto.koulutuskokonaisuudet()),
          toiminnot.add(user, importDto.toiminnot()));
    }
  }
}
