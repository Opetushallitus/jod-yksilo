/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.config.SessionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.mock.web.MockServletContext;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionEventHttpSessionListenerAdapter;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

class SessionConfigTest {

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
  }

  @Test
  void testSessionConfigBeansWithCloudProfile() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.getEnvironment().addActiveProfile("cloud");
    context.register(DefaultConfiguration.class);
    context.register(SessionConfig.class);
    context.setServletContext(new MockServletContext());
    context.refresh();

    assertThat(context.getBean(SessionEventHttpSessionListenerAdapter.class)).isNotNull();
    assertThat(context.getBean(SessionRepositoryFilter.class)).isNotNull();
    assertThat(context.getBean(SessionRepository.class)).isNotNull();
    assertThat(context.getBean(RedisSerializer.class)).isNotNull();
    assertThat(context.getBean(LettuceClientConfigurationBuilderCustomizer.class)).isNotNull();
  }

  @Configuration
  @EnableSpringHttpSession
  static class DefaultConfiguration {

    @Bean
    RedisConnectionFactory redisConnectionFactory() {
      return new LettuceConnectionFactory();
    }
  }
}
