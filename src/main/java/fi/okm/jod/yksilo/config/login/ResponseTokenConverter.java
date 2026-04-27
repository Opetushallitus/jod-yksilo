/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository.TunnistusData;
import fi.okm.jod.yksilo.service.onr.OppijanumeroService;
import fi.okm.jod.yksilo.service.onr.OppijanumeroServiceException;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@ConditionalOnBean(LoginConfig.class)
class ResponseTokenConverter implements Converter<ResponseToken, Saml2Authentication> {

  private final TransactionTemplate transactionTemplate;
  private final YksiloRepository yksilot;
  private final Converter<ResponseToken, Saml2Authentication> converter;
  private final JodAuthenticationProperties authenticationProperties;
  private final HttpSession session;
  private final OppijanumeroService oppijanumeroService;

  public ResponseTokenConverter(
      YksiloRepository yksilot,
      PlatformTransactionManager transactionManager,
      HttpSession session,
      JodAuthenticationProperties authenticationProperties,
      ObjectProvider<OppijanumeroService> oppijanumeroService) {
    this.converter = new ResponseAuthenticationConverter();
    this.yksilot = yksilot;

    var template = new TransactionTemplate(transactionManager);
    template.setTimeout(10);
    template.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    this.transactionTemplate = template;
    this.authenticationProperties = authenticationProperties;
    this.session = session;
    this.oppijanumeroService = oppijanumeroService.getIfAvailable();
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

      var jodUser =
          upsertUser(requireNonNull(authentication.getCredentials()), selectedLanguage, pid);

      var authorities = new ArrayList<>(authentication.getAuthorities());
      authorities.add(new SimpleGrantedAuthority("ROLE_FULL_USER"));

      return new Saml2AssertionAuthentication(
          jodUser,
          authentication.getCredentials(),
          authorities,
          authentication.getRelyingPartyRegistrationId());
    }

    throw new BadCredentialsException("Invalid response token");
  }

  /* package private for testing */
  @SuppressWarnings("java:S3776")
  JodSaml2Principal upsertUser(
      Saml2ResponseAssertionAccessor assertionAccessor,
      Kieli selectedLanguage,
      PersonIdentifierType pid) {

    var personId = getAttribute(assertionAccessor, pid.getAttribute()).orElse(null);
    if (personId == null || personId.isBlank()) {
      throw new BadCredentialsException("Invalid person identifier");
    }

    var etunimet =
        getAttribute(assertionAccessor, Attribute.FIRST_NAME)
            .orElseThrow(() -> new BadCredentialsException("Missing first name"));
    var kutsumanimi = getAttribute(assertionAccessor, Attribute.GIVEN_NAME).orElse(etunimet);
    var sukunimi =
        getAttribute(assertionAccessor, Attribute.SN)
            .or(() -> getAttribute(assertionAccessor, Attribute.FAMILY_NAME))
            .orElseThrow(() -> new BadCredentialsException("Missing family name"));

    var henkiloId = pid.asQualifiedIdentifier(personId);
    var tunnistusData = yksilot.findTunnistusDataByHenkiloId(henkiloId).orElse(null);

    final String oppijanumero;
    var onr = tunnistusData != null ? tunnistusData.oppijanumero() : null;
    if (pid == PersonIdentifierType.FIN && onr == null && oppijanumeroService != null) {
      try {
        if (tunnistusData != null) {
          log.atInfo()
              .addMarker(LogMarker.AUDIT)
              .addKeyValue("userId", tunnistusData.yksiloId())
              .log("Attempting to fetch oppijanumero from ONR for existing user");
        }
        onr = oppijanumeroService.fetchOppijanumero(personId, etunimet, kutsumanimi, sukunimi);
      } catch (OppijanumeroServiceException e) {
        // Ignore ONR errors and allow login without oppijanumero.
        log.atWarn()
            .addMarker(LogMarker.AUDIT)
            .addKeyValue("userId", tunnistusData == null ? null : tunnistusData.yksiloId())
            .log("Failed to fetch oppijanumero from ONR", e);
      }
    }
    oppijanumero = onr;

    return transactionTemplate.execute(
        _ -> {
          String effectiveOnr = oppijanumero;
          UUID yksiloId;

          // When the Suomi.fi account already exists and the fetched oppijanumero is already
          // associated with a different account (e.g. created via MPASSid), keep the accounts
          // separate instead of failing.
          if (tunnistusData != null
              && oppijanumero != null
              && tunnistusData.oppijanumero() == null) {
            var id = tunnistusData.yksiloId();
            var existing = yksilot.findTunnistusDataByOppijanumero(oppijanumero).orElse(null);
            if (existing != null && !existing.yksiloId().equals(id)) {
              log.atWarn()
                  .addMarker(LogMarker.AUDIT)
                  .addKeyValue("userId", id)
                  .addKeyValue("existingUserId", existing.yksiloId())
                  .log(
                      "Duplicate accounts detected: oppijanumero already associated with another account, proceeding without linking");
              effectiveOnr = null;
            }
          }

          if (tunnistusData != null
              && tunnistusData.equals(
                  new TunnistusData(
                      tunnistusData.yksiloId(), effectiveOnr, kutsumanimi, sukunimi))) {
            yksiloId = tunnistusData.yksiloId();
          } else {
            yksiloId = yksilot.upsertTunnistusData(henkiloId, effectiveOnr, kutsumanimi, sukunimi);
          }

          var yksilo =
              yksilot
                  .findById(yksiloId)
                  .map(
                      it ->
                          updateAttributes(it, pid, personId, assertionAccessor, selectedLanguage))
                  .orElseGet(
                      () -> {
                        log.atInfo()
                            .addMarker(LogMarker.AUDIT)
                            .log("Creating new user with id {}", yksiloId);
                        return new Yksilo(yksiloId);
                      });
          yksilot.save(yksilo);
          return new JodSaml2Principal(yksiloId, assertionAccessor.getAttributes());
        });
  }

  private static Yksilo updateAttributes(
      Yksilo yksilo,
      PersonIdentifierType pid,
      String personId,
      Saml2ResponseAssertionAccessor assertionAccessor,
      Kieli selectedLanguage) {
    if (yksilo.getValittuKieli() != null) {
      yksilo.setValittuKieli(selectedLanguage);
    }
    return switch (pid) {
      case FIN -> {
        var personIdentifier = FinnishPersonIdentifier.of(personId);
        if (yksilo.getSyntymavuosi() != null) {
          yksilo.setSyntymavuosi(personIdentifier.getBirthYear());
        }
        if (yksilo.getSukupuoli() != null) {
          yksilo.setSukupuoli(personIdentifier.getGender());
        }
        if (yksilo.getKotikunta() != null) {
          yksilo.setKotikunta(
              getAttribute(assertionAccessor, Attribute.KOTIKUNTA_KUNTANUMERO).orElse(null));
        }
        yield yksilo;
      }
      case EIDAS -> {
        if (yksilo.getSyntymavuosi() != null) {
          var birthDate =
              getAttribute(assertionAccessor, Attribute.DATE_OF_BIRTH)
                  .map(d -> LocalDate.parse(d).getYear())
                  .orElse(null);
          yksilo.setSyntymavuosi(birthDate);
        }
        yield yksilo;
      }
      default -> throw new IllegalStateException("Unexpected identifier type: " + pid);
    };
  }

  private static Optional<String> getAttribute(
      Saml2ResponseAssertionAccessor assertionAccessor, Attribute attribute) {
    return Optional.ofNullable(assertionAccessor.getFirstAttribute(attribute.getUri()));
  }
}
