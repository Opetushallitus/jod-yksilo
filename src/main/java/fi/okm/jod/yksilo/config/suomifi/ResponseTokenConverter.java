/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@ConditionalOnBean(Saml2LoginConfig.class)
class ResponseTokenConverter implements Converter<ResponseToken, Saml2Authentication> {

  private final TransactionTemplate transactionTemplate;
  private final YksiloRepository yksilot;
  private final Converter<ResponseToken, Saml2Authentication> converter;
  private final JodAuthenticationProperties authenticationProperties;
  private final HttpSession session;

  public ResponseTokenConverter(
      YksiloRepository yksilot,
      PlatformTransactionManager transactionManager,
      HttpSession session,
      JodAuthenticationProperties authenticationProperties) {
    this.converter = new ResponseAuthenticationConverter();
    this.yksilot = yksilot;

    var template = new TransactionTemplate(transactionManager);
    template.setTimeout(10);
    template.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    this.transactionTemplate = template;
    this.authenticationProperties = authenticationProperties;
    this.session = session;
  }

  @Override
  public Saml2Authentication convert(@NonNull ResponseToken responseToken) {
    if (converter.convert(responseToken) instanceof Saml2AssertionAuthentication authentication) {

      // https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/59116c3014bbb10001966f70
      // Tekninen rajapintakuvaus / Tunnistusvastaus / Tunnistustapahtuman vahvuus
      var level =
          URI.create(
              requireNonNull(
                  responseToken
                      .getResponse()
                      .getAssertions()
                      .getFirst()
                      .getAuthnStatements()
                      .getFirst()
                      .getAuthnContext()
                      .getAuthnContextClassRef()
                      .getURI()));

      var pid = authenticationProperties.getSupportedMethods().get(level);

      if (pid == null) {
        throw new BadCredentialsException("Unsupported authentication method: " + level);
      }

      // the language user has selected in the UI
      var selectedLanguage =
          switch (session.getAttribute(SessionLoginAttribute.LANG.getKey())) {
            case String s when s.equals("fi") -> Kieli.FI;
            case String s when s.equals("sv") -> Kieli.SV;
            case String s when s.equals("en") -> Kieli.EN;
            default -> null;
          };

      var jodUser = upsertUser(authentication.getCredentials(), selectedLanguage, pid);

      return new Saml2AssertionAuthentication(
          jodUser,
          authentication.getCredentials(),
          authentication.getAuthorities(),
          authentication.getRelyingPartyRegistrationId());
    }

    throw new BadCredentialsException("Invalid response token");
  }

  /* package private for testing */
  JodSaml2Principal upsertUser(
      Saml2ResponseAssertionAccessor assertionAccessor,
      Kieli selectedLanguage,
      PersonIdentifierType pid) {
    var personId = assertionAccessor.<String>getFirstAttribute(pid.getAttribute().getUri());
    if (personId == null || personId.isBlank()) {
      throw new BadCredentialsException("Invalid person identifier");
    }
    return transactionTemplate.execute(
        status -> {
          var id = yksilot.findIdByHenkiloId(pid.asQualifiedIdentifier(personId));
          var jodUser = new JodSaml2Principal(assertionAccessor.getAttributes(), id);
          yksilot.updateName(
              pid.asQualifiedIdentifier(personId), jodUser.givenName(), jodUser.familyName());

          var yksilo =
              yksilot
                  .findById(id)
                  .map(
                      it -> {
                        if (it.getValittuKieli() != null) {
                          it.setValittuKieli(selectedLanguage);
                        }
                        return switch (pid) {
                          case FIN -> {
                            var personIdentifier = FinnishPersonIdentifier.of(personId);
                            if (it.getSyntymavuosi() != null) {
                              it.setSyntymavuosi(personIdentifier.getBirthYear());
                            }
                            if (it.getSukupuoli() != null) {
                              it.setSukupuoli(personIdentifier.getGender());
                            }
                            if (it.getKotikunta() != null) {
                              it.setKotikunta(
                                  assertionAccessor.getFirstAttribute(
                                      Attribute.KOTIKUNTA_KUNTANUMERO.getUri()));
                            }
                            yield it;
                          }
                          case EIDAS -> {
                            var birthDate =
                                assertionAccessor.<String>getFirstAttribute(
                                    Attribute.DATE_OF_BIRTH.getUri());
                            if (it.getSyntymavuosi() != null) {
                              it.setSyntymavuosi(
                                  birthDate == null ? null : LocalDate.parse(birthDate).getYear());
                            }
                            yield it;
                          }
                        };
                      })
                  .orElseGet(
                      () -> {
                        log.atInfo()
                            .addMarker(LogMarker.AUDIT)
                            .log("Creating new user with id {}", id);
                        return new Yksilo(id);
                      });
          yksilot.save(yksilo);
          return jodUser;
        });
  }
}
