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
import static java.util.Objects.requireNonNullElse;
import static org.springframework.security.config.Customizer.withDefaults;

import fi.okm.jod.yksilo.config.LoginSuccessHandler;
import fi.okm.jod.yksilo.config.ProfileDeletionHandler;
import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.OpenSamlAssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jod.authentication.provider", havingValue = "suomifi")
@Slf4j
@RequiredArgsConstructor
public class Saml2LoginConfig {
  private final VetumaExtensionBuilder vetumaExtensionBuilder = new VetumaExtensionBuilder();
  private final YksiloService yksiloService;

  @Bean
  RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
      RelyingPartyProperties properties) {

    // missing:
    // - metadata signature validation (or loading from a trusted source)
    // - making the repository refreshable (to support metadata and credential rotation)

    var metadata =
        (OpenSamlAssertingPartyDetails)
            RelyingPartyRegistrations.fromMetadataLocation(properties.getIdpMetadataUri())
                .build()
                .getAssertingPartyMetadata();

    var descriptor = metadata.getEntityDescriptor().getIDPSSODescriptor(SAMLConstants.SAML20P_NS);

    var slo = getRedirectionEndpoint(descriptor.getSingleLogoutServices());
    var sso = getRedirectionEndpoint(descriptor.getSingleSignOnServices());

    // we want to use redirect binding, the default is arbitrary depending on the order in
    // the metadata descriptor
    metadata =
        metadata
            .mutate()
            .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
            .singleSignOnServiceLocation(requireNonNull(sso.getLocation()))
            .singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
            .singleLogoutServiceLocation(requireNonNull(slo.getLocation()))
            .singleLogoutServiceResponseLocation(
                requireNonNullElse(slo.getResponseLocation(), slo.getLocation()))
            .build();

    var cert = PemContent.of(properties.getCertificate());
    var key = PemContent.of(properties.getPrivateKey());

    var samlCredential =
        new Saml2X509Credential(
            key.getPrivateKey(),
            cert.getCertificates().getFirst(),
            Saml2X509CredentialType.DECRYPTION,
            Saml2X509CredentialType.SIGNING);

    return new InMemoryRelyingPartyRegistrationRepository(
        RelyingPartyRegistration.withAssertingPartyMetadata(metadata)
            .registrationId(properties.getRegistrationId())
            .nameIdFormat(NameIDType.TRANSIENT)
            .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
            .singleLogoutServiceLocation("{baseUrl}/logout/saml2/slo/{registrationId}")
            .singleLogoutServiceBinding(Saml2MessageBinding.POST)
            .signingX509Credentials(credentials -> credentials.add(samlCredential))
            .decryptionX509Credentials(credentials -> credentials.add(samlCredential))
            .build());
  }

  private static <T extends Endpoint> T getRedirectionEndpoint(Collection<T> endpoints) {
    return endpoints.stream()
        .filter(s -> s.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI))
        .findAny()
        .orElseThrow();
  }

  @Bean
  @SuppressWarnings("java:S4502")
  SecurityFilterChain samlSecurityFilterChain(
      HttpSecurity http,
      ResponseTokenConverter converter,
      Saml2AuthenticationRequestResolver authenticationRequestResolver,
      Saml2LogoutRequestResolver logoutRequestResolver,
      @Value("${jod.session.timeout}") Duration sessionTimeout)
      throws Exception {

    log.info("Configuring Suomi.fi-tunnistus");

    var redirectStrategy = new DefaultRedirectStrategy();
    redirectStrategy.setStatusCode(HttpStatus.SEE_OTHER);

    var loginSuccessHandler = new LoginSuccessHandler(sessionTimeout, redirectStrategy);
    var authenticationEventHandler = new AuthenticationEventHandler(redirectStrategy);
    var profileDeletionHandler = new ProfileDeletionHandler(yksiloService);

    var authProvider = new OpenSaml4AuthenticationProvider();
    authProvider.setResponseAuthenticationConverter(converter);

    return http.securityMatcher("/saml2/**", "/login/**", "/logout/**")
        .requestCache(RequestCacheConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(csrf -> csrf.ignoringRequestMatchers(request -> request.getSession(false) == null))
        .saml2Metadata(withDefaults())
        .saml2Login(
            login ->
                login
                    .loginPage("/login")
                    .successHandler(loginSuccessHandler)
                    .failureHandler(authenticationEventHandler)
                    .authenticationRequestResolver(authenticationRequestResolver)
                    .authenticationManager(new ProviderManager(authProvider)))
        .saml2Logout(
            logout -> {
              logout.logoutResponse(
                  response -> response.logoutUrl("/logout/saml2/slo/{registrationId}"));
              logout.logoutRequest(
                  request -> {
                    request.logoutUrl("/logout/saml2/slo/{registrationId}");
                    request.logoutRequestResolver(logoutRequestResolver);
                  });
            })
        .logout(
            logout -> {
              logout.addLogoutHandler(profileDeletionHandler);
              logout.logoutSuccessHandler(authenticationEventHandler);
            })
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; frame-ancestors 'self' https://tunnistautuminen.suomi.fi https://testi.apro.tunnistus.fi;")))
        .build();
  }

  @Bean
  Saml2AuthenticationRequestResolver authenticationRequestResolver(
      RelyingPartyRegistrationRepository registrations, JodAuthenticationProperties properties) {

    final var resolver = new OpenSaml4AuthenticationRequestResolver(registrations);
    final var builder = new AuthnContextBuilder();

    resolver.setAuthnRequestCustomizer(
        authnRequest -> {
          // https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/59116c3014bbb10001966f70
          // Tekninen rajapintakuvaus / Tunnistuspyyntö

          // Hyväksytyt tunnistusvälineet
          authnRequest
              .getAuthnRequest()
              .setRequestedAuthnContext(builder.build(properties.getSupportedMethods().keySet()));

          // Käyttöliittymän kieli
          resolveKieli(authnRequest.getRequest())
              .ifPresent(
                  kieli ->
                      authnRequest
                          .getAuthnRequest()
                          .setExtensions(vetumaExtensionBuilder.build(kieli)));
        });

    return resolver;
  }

  @Bean
  Saml2LogoutRequestResolver logoutRequestResolver(
      RelyingPartyRegistrationRepository registrations) {
    var resolver = new OpenSaml4LogoutRequestResolver(registrations);
    resolver.setParametersConsumer(
        parameters -> {
          final LogoutRequest logoutRequest = parameters.getLogoutRequest();
          // Suomi.fi tunnistus requires that the nameId format is set to transient
          logoutRequest.getNameID().setFormat(NameIDType.TRANSIENT);
          resolveKieli(parameters.getRequest())
              .ifPresent(kieli -> logoutRequest.setExtensions(vetumaExtensionBuilder.build(kieli)));
        });
    return resolver;
  }

  static Optional<Kieli> resolveKieli(HttpServletRequest req) {

    if (req.getSession(false) instanceof HttpSession session) {
      return Optional.ofNullable(
          switch (session.getAttribute(SessionLoginAttribute.LANG.getKey())) {
            case null -> null;
            case String s when s.equals("fi") -> Kieli.FI;
            case String s when s.equals("sv") -> Kieli.SV;
            default -> Kieli.EN;
          });
    }
    return Optional.empty();
  }

  static class AuthenticationEventHandler
      implements AuthenticationFailureHandler, LogoutSuccessHandler {

    private final RedirectStrategy redirectStrategy;

    public AuthenticationEventHandler(RedirectStrategy redirectStrategy) {
      this.redirectStrategy = redirectStrategy;
    }

    void handle(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {
      if (request.getSession(false) instanceof HttpSession s
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        // clear the temporary session used for SAML logout
        s.invalidate();
      }
      var queryParam = "";
      if (exception != null) {
        queryParam = "?error=AUTHENTICATION_FAILURE";
        log.atWarn()
            .addMarker(LogMarker.AUDIT)
            .log("Authentication failure: {}", exception.getMessage());
      }
      redirectStrategy.sendRedirect(request, response, "/" + queryParam);
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {
      handle(request, response, exception);
    }

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
      handle(request, response, null);
    }
  }
}
