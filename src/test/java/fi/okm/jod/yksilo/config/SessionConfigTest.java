/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.SessionEventHttpSessionListenerAdapter;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

@Testcontainers
class SessionConfigTest {

  @Container
  static GenericContainer<?> redisContainer =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Test
  void testSessionConfigBeans() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(DefaultConfiguration.class);
    context.register(SessionConfig.class);
    context.setServletContext(new MockServletContext());
    context.refresh();

    assertThat(context.getBean(SessionEventHttpSessionListenerAdapter.class)).isNotNull();
    assertThat(context.getBean(SessionRepositoryFilter.class)).isNotNull();
    assertThat(context.getBean(SessionRepository.class)).isNotNull();
    assertThat(context.getBean(RedisSerializer.class)).isNotNull();
    assertThrows(
        NoSuchBeanDefinitionException.class,
        () -> context.getBean(LettuceClientConfigurationBuilderCustomizer.class));

    context.close();
  }

  @Test
  void testSessionConfigBeansWithCloudProfile() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.getEnvironment().addActiveProfile("cloud");
    context
        .getEnvironment()
        .getSystemProperties()
        .put("aws.region", Region.regions().getFirst().id());
    context.register(DefaultConfiguration.class);
    context.register(SessionConfig.class);
    context.setServletContext(new MockServletContext());
    context.refresh();

    assertThat(context.getBean(SessionEventHttpSessionListenerAdapter.class)).isNotNull();
    assertThat(context.getBean(SessionRepositoryFilter.class)).isNotNull();
    assertThat(context.getBean(SessionRepository.class)).isNotNull();
    assertThat(context.getBean(RedisSerializer.class)).isNotNull();
    assertThat(context.getBean(LettuceClientConfigurationBuilderCustomizer.class)).isNotNull();

    context.close();
  }

  @ParameterizedTest
  @MethodSource("scopes")
  void shouldRoundTripOauth2AuthorizedClientWithScopes(List<String> scopes) {
    var config = new SessionConfig();
    config.setBeanClassLoader(getClass().getClassLoader());
    var serializer = config.springSessionDefaultRedisSerializer();
    var registrationBuilder =
        ClientRegistration.withRegistrationId("tmt-vienti")
            .clientId("client-id")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/oauth2/response/{registrationId}")
            .authorizationUri("https://example.com/authorize")
            .tokenUri("https://example.com/token")
            .clientName("tmt-vienti")
            .scope(scopes);
    var registration = registrationBuilder.build();
    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            Instant.now(),
            Instant.now().plusSeconds(300));
    Map<String, Object> sessionAttribute = new HashMap<>();
    sessionAttribute.put(
        "tmt-vienti", new OAuth2AuthorizedClient(registration, "principal", accessToken));

    var deserialized = serializer.deserialize(serializer.serialize(sessionAttribute));

    assertThat(deserialized).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) deserialized).get("tmt-vienti"))
        .isInstanceOfSatisfying(
            OAuth2AuthorizedClient.class,
            client -> {
              assertThat(client.getClientRegistration().getScopes())
                  .containsExactlyElementsOf(scopes);
              assertThat(
                      client
                          .getClientRegistration()
                          .getProviderDetails()
                          .getConfigurationMetadata())
                  .isEmpty();
              assertThat(client.getAccessToken().getTokenValue()).isEqualTo("access-token");
            });
  }

  private static Stream<List<String>> scopes() {
    return Stream.of(List.of(), List.of("openid", "profile"));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebSecurity
  @EnableRedisHttpSession
  static class DefaultConfiguration {

    @Bean
    @Profile("cloud")
    public AwsCredentialsProvider awsCredentialsProvider() {
      return DefaultCredentialsProvider.create();
    }

    @Bean
    @Profile("cloud")
    public AwsRegionProvider regionProvider() {
      return DefaultAwsRegionProviderChain.builder().build();
    }

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
      return new LettuceConnectionFactory(
          redisContainer.getHost(), redisContainer.getFirstMappedPort());
    }
  }
}
