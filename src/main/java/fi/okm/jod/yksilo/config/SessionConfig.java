/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import fi.okm.jod.yksilo.config.login.JodOidcPrincipal;
import fi.okm.jod.yksilo.config.login.JodSaml2Principal;
import fi.okm.jod.yksilo.controller.KeskusteluController.InferenceSession;
import fi.okm.jod.yksilo.domain.JodUser;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration(proxyBeanMethods = false)
@SuppressWarnings("java:S4544")
public class SessionConfig implements BeanClassLoaderAware {

  private ClassLoader loader;

  @JsonTypeInfo(use = Id.CLASS)
  interface SessionMixin {}

  @Override
  public void setBeanClassLoader(@NonNull ClassLoader classLoader) {
    this.loader = classLoader;
  }

  @Bean
  public GenericConversionService springSessionConversionService() {
    // Create a custom ObjectMapper that uses Spring Security’s Jackson modules.

    var validatorBuilder =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(JodUser.class)
            .allowIfSubType(URL.class)
            .allowIfSubType(Collections.emptySet().getClass());

    var mapper =
        JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(this.loader, validatorBuilder))
            .addMixIn(InferenceSession.class, SessionMixin.class)
            .addMixIn(JodOidcPrincipal.class, JodOidcPrincipalMixin.class)
            .addMixIn(JodSaml2Principal.class, JodSaml2PrincipalMixin.class)
            .build();
    var conversionService = new GenericConversionService();
    conversionService.addConverter(Object.class, byte[].class, mapper::writeValueAsBytes);
    conversionService.addConverter(
        byte[].class, Object.class, bytes -> mapper.readValue(bytes, Object.class));
    return conversionService;
  }

  @JsonTypeInfo(use = Id.CLASS)
  @JsonIncludeProperties({"id", "idToken"})
  public abstract static class JodOidcPrincipalMixin {
    @JsonCreator
    JodOidcPrincipalMixin(
        @JsonProperty("id") UUID id, @JsonProperty("idToken") OidcIdToken idToken) {}
  }

  @JsonTypeInfo(use = Id.CLASS)
  @JsonIncludeProperties({"id", "attributes"})
  public abstract static class JodSaml2PrincipalMixin {
    @JsonCreator
    JodSaml2PrincipalMixin(
        @JsonProperty("id") UUID id,
        @JsonProperty("attributes") Map<String, List<Object>> attributes) {}
  }
}
