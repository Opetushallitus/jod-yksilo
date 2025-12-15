/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.tmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SecurityConfig;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.config.tmt.TmtSecurityConfig;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.tmt.TmtExportService;
import fi.okm.jod.yksilo.service.tmt.TmtImportService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

@WebMvcTest(TmtProfileController.class)
@Import({
  ErrorInfoFactory.class,
  MappingConfig.class,
  SecurityConfig.class,
  TmtSecurityConfig.class
})
@EnableConfigurationProperties(TmtConfiguration.class)
@TestPropertySource(
    properties = {
      "jod.tmt.enabled=true",
    })
class TmtProfileControllerTest {
  @Autowired private MockMvc mvc;

  @MockitoBean private TmtExportService tmtExportService;
  @MockitoBean private TmtImportService tmtImportService;
  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository;

  @BeforeEach
  void setUp() {
    when(tmtExportService.canExport(any())).thenReturn(true);
  }

  @Test
  void shouldFailIfUnauthorized() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/vienti")).andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("test")
  void shouldNotExportProfileWithoutAuthorization() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/vienti").with(csrf())).andExpect(status().is(400));
  }

  @Test
  @WithUserDetails("test")
  void shouldExportProfile() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/vienti").with(csrf()).with(oauth2Client("tmt-vienti")))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  @WithUserDetails("test")
  void shouldImportProfile() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/haku").with(csrf()).with(oauth2Client("tmt-haku")))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  @WithUserDetails("test")
  void shouldNotImportProfileWithoutAuthorization() throws Exception {
    mvc.perform(post("/api/integraatiot/tmt/haku").with(csrf())).andExpect(status().is(400));
  }

  @TestConfiguration
  static class Config {
    @Bean
    UserDetailsService mockUserDetailsService() {
      return username -> new MockJodUserImpl(username, UUID.nameUUIDFromBytes(username.getBytes()));
    }

    @Bean
    RestClient.Builder restClientBuilder() {
      return RestClient.builder();
    }
  }
}
