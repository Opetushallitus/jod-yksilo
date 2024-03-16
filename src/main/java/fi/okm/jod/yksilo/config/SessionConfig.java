/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.okm.jod.yksilo.config.elasticache.IamAuthTokenRequest;
import fi.okm.jod.yksilo.config.elasticache.RedisIamAuthCredentialsProvider;
import io.lettuce.core.RedisCredentialsProvider;
import java.util.Objects;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.RedisCredentialsProviderFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Configuration(proxyBeanMethods = false)
@EnableRedisIndexedHttpSession
public class SessionConfig<S extends Session> implements BeanClassLoaderAware {

  @Value("${spring.data.redis.cache-name:}")
  private String cacheName;

  private ClassLoader loader;

  @Override
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
  public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
    this.loader = classLoader;
  }

  @Bean
  public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
    // Create a custom ObjectMapper that uses Spring Security’s Jackson modules.
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
    return new GenericJackson2JsonRedisSerializer(mapper);
  }

  @Bean
  public SpringSessionBackedSessionRegistry<S> sessionRegistry(
      FindByIndexNameSessionRepository<S> sessionRepository) {
    return new SpringSessionBackedSessionRegistry<>(sessionRepository);
  }

  @Bean
  @Order(2)
  public SecurityFilterChain sessionFilterChain(
      HttpSecurity http, SpringSessionBackedSessionRegistry<S> sessionRegistry) throws Exception {
    return http.sessionManagement(
            (sessionManagement) ->
                sessionManagement.maximumSessions(1).sessionRegistry(sessionRegistry))
        .build();
  }

  @Bean
  @ConditionalOnBean(value = {AwsCredentialsProvider.class, Region.class})
  LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer(
      AwsCredentialsProvider awsCredentialsProvider, Region region) {
    return builder ->
        builder.redisCredentialsProviderFactory(
            new RedisCredentialsProviderFactory() {
              @Override
              public RedisCredentialsProvider createCredentialsProvider(
                  @NonNull RedisConfiguration redisConfiguration) {
                if (!Objects.equals(cacheName, "")
                    && redisConfiguration
                        instanceof RedisStandaloneConfiguration redisStandaloneConfiguration) {
                  // Custom implementation of RedisCredentialsProvider for IAM Authentication.

                  // References:
                  // https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth-iam.html#auth-iam-Connecting
                  // https://github.com/aws-samples/elasticache-iam-auth-demo-app/tree/main

                  // The username is the same as the user id.
                  String username = redisStandaloneConfiguration.getUsername();

                  IamAuthTokenRequest iamAuthTokenRequest =
                      new IamAuthTokenRequest(username, cacheName, region);

                  return new RedisIamAuthCredentialsProvider(
                      username, iamAuthTokenRequest, awsCredentialsProvider);
                } else {
                  return RedisCredentialsProviderFactory.super.createCredentialsProvider(
                      redisConfiguration);
                }
              }
            });
  }
}
