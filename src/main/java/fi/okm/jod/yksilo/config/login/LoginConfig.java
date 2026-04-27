/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static fi.okm.jod.yksilo.config.SessionLoginAttribute.LANG;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.security.config.Customizer.withDefaults;

import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.core.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.OpenSamlAssertingPartyDetails;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml5AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml5LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jod.authentication.provider", havingValue = "suomifi")
@Slf4j
@RequiredArgsConstructor
public class LoginConfig {
  private final VetumaExtensionBuilder vetumaExtensionBuilder = new VetumaExtensionBuilder();
  private final YksiloService yksiloService;

  static final String MPASSID_REGISTRATION_ID = "mpassid";

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
  SecurityFilterChain loginFilterChain(
      HttpSecurity http,
      ResponseTokenConverter converter,
      Saml2AuthenticationRequestResolver authenticationRequestResolver,
      Saml2LogoutRequestResolver logoutRequestResolver,
      MpassidProperties mpassidProperties,
      ObjectProvider<MpassidOidcUserConverter> mpassidOidcUserConverter,
      @Value("${jod.session.timeout}") Duration sessionTimeout) {

    log.info("Configuring Suomi.fi-tunnistus");

    var redirectStrategy = new DefaultRedirectStrategy();
    redirectStrategy.setStatusCode(HttpStatus.SEE_OTHER);

    var mpassidLogoutUri =
        mpassidProperties.isEnabled() ? mpassidProperties.getOidc().getLogoutUri() : null;

    var loginSuccessHandler = new LoginSuccessHandler(sessionTimeout, redirectStrategy);
    var authenticationEventHandler =
        new AuthenticationEventHandler(redirectStrategy, mpassidLogoutUri);
    var profileDeletionHandler = new ProfileDeletionHandler(yksiloService);

    http.securityMatcher("/saml2/**", "/login/**", "/logout/**", "/oauth2/*/mpassid")
        .requestCache(RequestCacheConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(csrf -> csrf.ignoringRequestMatchers(request -> request.getSession(false) == null));

    configureSuomiFi(
        http,
        converter,
        authenticationRequestResolver,
        logoutRequestResolver,
        loginSuccessHandler,
        authenticationEventHandler);

    if (mpassidProperties.isEnabled()) {
      configureMpassid(
          http,
          mpassidProperties,
          mpassidOidcUserConverter.getObject(),
          loginSuccessHandler,
          authenticationEventHandler);
    }

    http.logout(
            logout -> {
              logout.addLogoutHandler(profileDeletionHandler);
              logout.logoutSuccessHandler(authenticationEventHandler);
            })
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; frame-ancestors 'self' https://tunnistautuminen.suomi.fi https://testi.apro.tunnistus.fi;")));

    return http.build();
  }

  private void configureSuomiFi(
      HttpSecurity http,
      ResponseTokenConverter converter,
      Saml2AuthenticationRequestResolver authenticationRequestResolver,
      Saml2LogoutRequestResolver logoutRequestResolver,
      LoginSuccessHandler loginSuccessHandler,
      AuthenticationEventHandler authenticationEventHandler) {

    var authProvider = new OpenSaml5AuthenticationProvider();
    authProvider.setResponseAuthenticationConverter(converter);

    http.saml2Metadata(withDefaults())
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
            });
  }

  @Bean
  Saml2AuthenticationRequestResolver authenticationRequestResolver(
      RelyingPartyRegistrationRepository registrations, JodAuthenticationProperties properties) {

    final var resolver = new OpenSaml5AuthenticationRequestResolver(registrations);
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
    var resolver = new OpenSaml5LogoutRequestResolver(registrations);
    resolver.setParametersConsumer(
        parameters -> {
          final LogoutRequest logoutRequest = parameters.getLogoutRequest();
          // Suomi.fi tunnistus requires that the nameId format is set to transient
          logoutRequest.getNameID().setFormat(NameIDType.TRANSIENT);
          resolveKieli(parameters.getRequest())
              .ifPresent(
                  kieli -> {
                    parameters
                        .getRequest()
                        .getSession()
                        .setAttribute(LANG.getKey(), kieli.toString());
                    logoutRequest.setExtensions(vetumaExtensionBuilder.build(kieli));
                  });
        });
    return resolver;
  }

  private void configureMpassid(
      HttpSecurity http,
      MpassidProperties mpassidProperties,
      MpassidOidcUserConverter userConverter,
      LoginSuccessHandler loginSuccessHandler,
      AuthenticationEventHandler authenticationEventHandler) {

    log.info("Configuring MPASSid OIDC login: {}", mpassidProperties.getOidc().getIssuerUri());
    var mpassidRegistration = mpassidClientRegistration(mpassidProperties);
    var oidcUserService = new OidcUserService();
    oidcUserService.setOidcUserConverter(userConverter);

    http.oauth2Login(
        oauth2 ->
            oauth2
                .clientRegistrationRepository(
                    new InMemoryClientRegistrationRepository(mpassidRegistration))
                .authorizedClientRepository(new NoopOauth2AuthorizedClientRepository())
                .loginProcessingUrl("/oauth2/response/mpassid")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .successHandler(loginSuccessHandler)
                .failureHandler(authenticationEventHandler));
  }

  ClientRegistration mpassidClientRegistration(MpassidProperties properties) {
    var oidc = properties.getOidc();

    return ClientRegistration.withRegistrationId(MPASSID_REGISTRATION_ID)
        .clientName(MPASSID_REGISTRATION_ID)
        .issuerUri(oidc.getIssuerUri().toString())
        .authorizationUri(oidc.getAuthorizationUri().toString())
        .tokenUri(oidc.getTokenUri().toString())
        .jwkSetUri(oidc.getJwksUri().toString())
        .clientId(oidc.getClientId())
        .clientSecret(oidc.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/oauth2/response/{registrationId}")
        .scope("openid")
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "jod.mpassid.enabled", havingValue = "true")
  JwtDecoderFactory<ClientRegistration> cachingJwtDecoderFactory() {
    // Implementation note:
    // By default, a new JwtDecoder is created for each authentication which is expensive because it
    // involves fetching the JWK set. In addition, the JwtDecoder caches the keys
    // (JWKSet) for a short time. Customizing the caching behavior would require creating a fully
    // custom JwtDecoderFactory implementation, since it is not possible to customize how the
    // actual decoder is created.

    final var delegate = new OidcIdTokenDecoderFactory();
    final var cache = Caffeine.newBuilder().maximumSize(10).<String, JwtDecoder>build();
    final var cacheKey =
        (Function<ClientRegistration, String>)
            reg ->
                reg.getRegistrationId()
                    + ":"
                    + Objects.hash(
                        reg.getProviderDetails().getIssuerUri(),
                        reg.getClientId(),
                        reg.getClientSecret());

    return clientRegistration -> {
      var key = cacheKey.apply(clientRegistration);
      return cache.get(key, _ -> delegate.createDecoder(clientRegistration));
    };
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

    return Optional.ofNullable(
        switch (req.getParameter("lang")) {
          case null -> null;
          case "fi" -> Kieli.FI;
          case "sv" -> Kieli.SV;
          default -> Kieli.EN;
        });
  }

  static class AuthenticationEventHandler
      implements AuthenticationFailureHandler, LogoutSuccessHandler {

    private final RedirectStrategy redirectStrategy;
    private final URI mpassidLogoutUri;

    public AuthenticationEventHandler(RedirectStrategy redirectStrategy, URI mpassidLogoutUri) {
      this.redirectStrategy = redirectStrategy;
      this.mpassidLogoutUri = mpassidLogoutUri;
    }

    void handle(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {
      handle(request, response, exception, null);
    }

    void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception,
        Authentication authentication)
        throws IOException {

      var returnUrl =
          ServletUriComponentsBuilder.fromRequestUri(request).replacePath(request.getContextPath());
      resolveKieli(request).ifPresent(k -> returnUrl.pathSegment(k.getKoodi()));
      returnUrl.path("/");

      if (request.getSession(false) instanceof HttpSession s && authentication == null) {
        // clear the temporary session used for SAML logout
        s.invalidate();
      }

      String targetUrl;
      if (authentication != null
          && authentication.getPrincipal() instanceof OidcUser
          && mpassidLogoutUri != null
          && mpassidLogoutUri.isAbsolute()) {
        targetUrl =
            UriComponentsBuilder.fromUri(mpassidLogoutUri)
                .queryParam("return", returnUrl.toUriString())
                .build()
                .toUriString();
      } else {
        if (exception != null) {
          log.atWarn()
              .addMarker(LogMarker.AUDIT)
              .log("Authentication failure: {}", exception.getMessage());
          returnUrl.queryParam("error", "AUTHENTICATION_FAILURE");
        }
        targetUrl = returnUrl.toUriString();
      }

      redirectStrategy.sendRedirect(request, response, targetUrl);
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
      handle(request, response, null, authentication);
    }
  }
}
