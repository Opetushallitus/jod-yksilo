/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Import({TmtExportService.class, TmtConfiguration.class})
@TestPropertySource(
    properties = {
      "jod.tmt.enabled=true",
      "jod.tmt.export-api.kipa-subscription-key=key",
      "jod.tmt.export-api.api-url=/v1/profile",
    })
@Slf4j
class TmtExportServiceTest extends AbstractServiceTest {
  @Autowired private TmtExportService tmtExportService;
  @Autowired private TestConfig testConfig;

  @TestConfiguration
  static class TestConfig {
    private final RestClient.Builder clientBuilder = RestClient.builder();
    private final MockRestServiceServer mockServer =
        MockRestServiceServer.bindTo(clientBuilder).build();

    @Bean
    RestClient tmtRestClient() {
      return clientBuilder.build();
    }

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
      return new MappingJackson2HttpMessageConverter();
    }
  }

  @BeforeEach
  void setUp() {
    testConfig.mockServer.reset();
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  void shouldExportProfile() {
    testConfig
        .mockServer
        .expect(requestTo("/v1/profile"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header("Authorization", "Bearer token"))
        .andExpect(header("KIPA-Subscription-Key", "key"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess());

    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.MAX);
    assertDoesNotThrow(() -> tmtExportService.export(user, accessToken));
  }
}
