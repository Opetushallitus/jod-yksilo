/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "jod.authentication")
@Getter
public class JodAuthenticationProperties {

  private final String provider;
  private final Map<URI, PersonIdentifierType> supportedMethods;

  @ConstructorBinding
  JodAuthenticationProperties(
      String provider, Map<PersonIdentifierType, List<URI>> supportedMethods) {
    this.provider = provider;
    // Reversing the map is intentional: PersonIdentifier -> URI is easier to override
    // using SSM parameters, but URI -> PersonIdentifier is more convenient to use
    this.supportedMethods =
        supportedMethods == null
            ? Map.of()
            : supportedMethods.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(uri -> Map.entry(uri, e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
