/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.tmt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl;
import fi.okm.jod.yksilo.config.tmt.TmtAuthorizationRepository;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.tmt.TmtExportService;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

@WebMvcTest(value = TmtExportController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class, TmtAuthorizationRepository.class})
@EnableConfigurationProperties(TmtConfiguration.class)
@TestPropertySource(
    properties = {
      "jod.tmt.enabled=true",
      "jod.tmt.kipa-subscription-key=apikey",
      "jod.tmt.api-url=http://api.local",
      "jod.tmt.authorization-url=http://authorization.local",
      "jod.tmt.token-issuer=issuer",
    })
class TmtExportControllerTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private TmtExportService tmtExportService;

  @BeforeEach
  void setUp() {
    when(tmtExportService.canExport(any())).thenReturn(true);
  }

  @Test
  @WithUserDetails("test")
  void shouldNotExportProfileWithoutAuthorization() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/vienti/{id}", UUID.randomUUID()).with(csrf()))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithUserDetails("test")
  void shouldExportProfile() throws Exception {
    var session = new MockHttpSession();

    // simulate authorization
    // 1. get redirect to authorization url
    var result =
        mvc.perform(
                get("/api/integraatiot/tmt/vienti/auktorisointi")
                    .param("callback", "/callback-route")
                    .session(session))
            .andExpect(status().isFound())
            .andReturn();
    assertNotNull(result.getResponse().getRedirectedUrl());
    var responseUrl =
        URI.create(
                UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
                    .build()
                    .getQueryParams()
                    .get("redirectUrl")
                    .getFirst())
            .getPath();

    // 2. simulate authorization response
    var token =
        new PlainJWT(
                new JWTClaimsSet.Builder()
                    .issuer("issuer")
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build())
            .serialize();

    var response =
        mvc.perform(get(responseUrl).queryParam("token", token).session(session))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    // call export
    assertThat(response.getResponse().getRedirectedUrl()).startsWith("/callback-route");
    var exportId =
        UriComponentsBuilder.fromUriString(response.getResponse().getRedirectedUrl())
            .build()
            .getQueryParams()
            .get("exportId")
            .getFirst();

    mvc.perform(post("/api/integraatiot/tmt/vienti/{id}", exportId).with(csrf()).session(session))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  @WithUserDetails("test")
  void shouldRejectInvalidToken() throws Exception {
    var session = new MockHttpSession();

    // simulate authorization
    // 1. get redirect to authorization url
    var result =
        mvc.perform(
                get("/api/integraatiot/tmt/vienti/auktorisointi")
                    .param("callback", "/callback-route")
                    .session(session))
            .andExpect(status().isFound())
            .andReturn();
    assertNotNull(result.getResponse().getRedirectedUrl());
    var responseUrl =
        URI.create(
                UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
                    .build()
                    .getQueryParams()
                    .get("redirectUrl")
                    .getFirst())
            .getPath();

    // 2. simulate authorization response with invalid token
    var token = "invalid-token";

    var response =
        mvc.perform(get(responseUrl).queryParam("token", token).session(session))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    // check that the response redirects to the callback with an error
    assertThat(response.getResponse().getRedirectedUrl())
        .startsWith("/callback-route?error=authorization_failed");
  }

  @TestConfiguration
  static class Config {
    @Bean
    UserDetailsService mockUserDetailsService() {
      return username -> new MockJodUserImpl(username, UUID.nameUUIDFromBytes(username.getBytes()));
    }
  }
}
