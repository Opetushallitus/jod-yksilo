/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.elasticache.IamAuthTokenRequest;
import fi.okm.jod.yksilo.config.elasticache.RedisIamAuthCredentialsProvider;
import fi.okm.jod.yksilo.controller.KeskusteluController.InferenceSession;
import io.lettuce.core.RedisCredentialsProvider;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration.WithAuthentication;
import org.springframework.data.redis.connection.lettuce.RedisCredentialsProviderFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

@Configuration(proxyBeanMethods = false)
public class SessionConfig implements BeanClassLoaderAware {

  @Value("${spring.data.redis.cache-name:}")
  private String cacheName;

  private ClassLoader loader;

  @JsonTypeInfo(use = Id.CLASS)
  interface SessionMixin {}

  @Override
  public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
    this.loader = classLoader;
  }

  @Bean
  public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
    // Create a custom ObjectMapper that uses Spring Security’s Jackson modules.
    var mapper = new ObjectMapper();
    mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
    mapper.addMixIn(InferenceSession.class, SessionMixin.class);
    return new GenericJackson2JsonRedisSerializer(mapper);
  }

  @Bean
  @Profile("cloud")
  LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer(
      AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider regionProvider) {
    return builder ->
        builder.redisCredentialsProviderFactory(
            new RedisCredentialsProviderFactory() {
              @Override
              public RedisCredentialsProvider createCredentialsProvider(
                  @NonNull RedisConfiguration redisConfiguration) {
                if (StringUtils.hasLength(cacheName)
                    && redisConfiguration instanceof WithAuthentication authentication) {
                  // Custom implementation of RedisCredentialsProvider for IAM Authentication.

                  // References:
                  // https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth-iam.html#auth-iam-Connecting
                  // https://github.com/aws-samples/elasticache-iam-auth-demo-app/tree/main

                  // The username is the same as the user id.
                  String username = authentication.getUsername();

                  IamAuthTokenRequest iamAuthTokenRequest =
                      new IamAuthTokenRequest(username, cacheName, regionProvider.getRegion());

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
