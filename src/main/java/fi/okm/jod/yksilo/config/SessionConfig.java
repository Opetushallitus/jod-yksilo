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
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession
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
}
