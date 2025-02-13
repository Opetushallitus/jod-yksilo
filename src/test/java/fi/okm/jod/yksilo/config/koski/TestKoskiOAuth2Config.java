/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import static org.mockito.Mockito.mock;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestKoskiOAuth2Config {

  @Bean
  public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean(KoskiRestClientConfig.RESTCLIENT_ID)
  public RestClient restClient() {
    return RestClient.builder().build();
  }

  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager() {
    return mock(OAuth2AuthorizedClientManager.class);
  }

  @Bean
  public SslBundles sslBundles() {
    return mock(SslBundles.class);
  }
}
