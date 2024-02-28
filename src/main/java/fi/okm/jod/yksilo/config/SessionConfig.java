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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.RedisCredentialsProviderFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession
@Slf4j
public class SessionConfig implements BeanClassLoaderAware {
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
  @Profile("cloud")
  LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
    return builder ->
        builder.redisCredentialsProviderFactory(
            new RedisCredentialsProviderFactory() {
              @Override
              public RedisCredentialsProvider createCredentialsProvider(
                  @NonNull RedisConfiguration redisConfiguration) {
                if (redisConfiguration
                    instanceof RedisStandaloneConfiguration redisStandaloneConfiguration) {
                  // Custom implementation of RedisCredentialsProvider for IAM Authentication.

                  // References:
                  // https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth-iam.html#auth-iam-Connecting
                  // https://github.com/aws-samples/elasticache-iam-auth-demo-app/tree/main

                  // The username is the same as the user id.
                  String username = redisStandaloneConfiguration.getUsername();

                  // The cache name is the subdomain of the host name without the last part.
                  String hostName = redisStandaloneConfiguration.getHostName();
                  String subdomain = hostName.split("\\.")[0];
                  String cacheName = subdomain.substring(0, subdomain.lastIndexOf("-"));

                  // The region is same as this application's region.
                  Region region = new DefaultAwsRegionProviderChain().getRegion();

                  log.debug(
                      "Using IAM Authentication for Redis with username: {}, cacheName: {}, region: {}",
                      username,
                      cacheName,
                      region);

                  AwsCredentialsProvider awsCredentialsProvider =
                      DefaultCredentialsProvider.create();

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
