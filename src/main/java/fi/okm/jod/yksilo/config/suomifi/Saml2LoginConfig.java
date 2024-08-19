/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static org.springframework.security.config.Customizer.withDefaults;

import fi.okm.jod.yksilo.domain.Kieli;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml4AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jod.authentication.provider", havingValue = "suomifi")
@Slf4j
public class Saml2LoginConfig {
  private final VetumaExtensionBuilder vetumaExtensionBuilder = new VetumaExtensionBuilder();

  @Bean
  @SuppressWarnings("java:S4502")
  SecurityFilterChain samlSecurityFilterChain(
      HttpSecurity http,
      ResponseTokenConverter converter,
      Saml2AuthenticationRequestResolver authenticationRequestResolver,
      Saml2LogoutRequestResolver logoutRequestResolver)
      throws Exception {

    log.info("Configuring Suomi.fi-tunnistus");

    var redirectStrategy = new DefaultRedirectStrategy();
    redirectStrategy.setStatusCode(HttpStatus.SEE_OTHER);

    var loginSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/");
    loginSuccessHandler.setRedirectStrategy(redirectStrategy);
    loginSuccessHandler.setAlwaysUseDefaultTargetUrl(true);
    var authenticationEventHandler = new AuthenticationEventHandler(redirectStrategy);

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
        .logout(logout -> logout.logoutSuccessHandler(authenticationEventHandler))
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
    var lang =
        switch (req.getParameter("lang")) {
          case null -> null;
          case "fi" -> Kieli.FI;
          case "sv" -> Kieli.SV;
          default -> Kieli.EN;
        };
    return Optional.ofNullable(lang);
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
      if (exception != null) {
        log.warn("Authentication failure: {}", exception.getMessage());
      }
      redirectStrategy.sendRedirect(request, response, "/");
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
