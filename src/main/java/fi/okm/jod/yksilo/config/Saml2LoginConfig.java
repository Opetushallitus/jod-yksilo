/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.opensaml.saml.saml2.core.NameIDType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class Saml2LoginConfig {
  @Bean
  SecurityFilterChain samlSecurityFilterChain(
      HttpSecurity http, Saml2LogoutRequestResolver logoutRequestResolver) throws Exception {
    return http.saml2Login(login -> login.defaultSuccessUrl("/api/v1/user", true))
        .saml2Logout(
            logout -> {
              logout.logoutResponse(
                  response -> response.logoutUrl("/logout/saml2/slo/{registrationId}"));
              logout.logoutRequest(request -> request.logoutRequestResolver(logoutRequestResolver));
            })
        .saml2Metadata(withDefaults())
        .build();
  }

  @Bean
  Saml2LogoutRequestResolver logoutRequestResolver(
      RelyingPartyRegistrationRepository registrations) {
    var resolver = new OpenSaml4LogoutRequestResolver(registrations);

    // Suomi.fi tunnistus requires that the nameId format is set to transient
    resolver.setParametersConsumer(
        (parameters) -> parameters.getLogoutRequest().getNameID().setFormat(NameIDType.TRANSIENT));
    return resolver;
  }
}
