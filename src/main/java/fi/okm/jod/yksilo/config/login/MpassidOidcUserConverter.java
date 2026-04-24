/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static java.util.Objects.requireNonNull;
import static org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.OppijanumeroUtils;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserSource;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@ConditionalOnProperty(name = "jod.mpassid.enabled", havingValue = "true")
public class MpassidOidcUserConverter implements Converter<OidcUserSource, OidcUser> {

  private final TransactionTemplate transactionTemplate;
  private final YksiloRepository yksilot;
  private final MpassidProperties properties;

  public MpassidOidcUserConverter(
      YksiloRepository yksilot,
      PlatformTransactionManager transactionManager,
      MpassidProperties properties) {
    this.yksilot = yksilot;
    this.properties = properties;

    var template = new TransactionTemplate(transactionManager);
    template.setTimeout(10);
    template.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    this.transactionTemplate = template;
  }

  @Override
  public OidcUser convert(OidcUserSource userSource) {
    var idToken = userSource.getUserRequest().getIdToken();

    var onr = idToken.getClaimAsString(Attribute.OPPIJANUMERO_CLAIM.getUri());
    if (onr == null || onr.isBlank()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(INVALID_TOKEN, "Missing oppijanumero claim", null));
    }

    String qualifiedOppijanumero;
    try {
      qualifiedOppijanumero =
          PersonIdentifierType.ONR.asQualifiedIdentifier(
              OppijanumeroUtils.validate(
                  onr, properties.getOnrOidPrefix(), properties.isValidateOnrChecksum()));
    } catch (IllegalArgumentException e) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(INVALID_TOKEN, "Invalid oppijanumero", null), e);
    }

    var principal =
        transactionTemplate.execute(
            _ -> {
              var yksiloId =
                  yksilot.upsertTunnistusData(
                      null, qualifiedOppijanumero, idToken.getGivenName(), idToken.getFamilyName());

              if (yksilot.findById(yksiloId).isEmpty()) {
                log.atInfo()
                    .addMarker(LogMarker.AUDIT)
                    .log("Creating new MPASSid user with id {}", yksiloId);
                yksilot.save(new Yksilo(yksiloId));
              }
              return new JodOidcPrincipal(yksiloId, idToken);
            });

    return requireNonNull(principal);
  }
}
