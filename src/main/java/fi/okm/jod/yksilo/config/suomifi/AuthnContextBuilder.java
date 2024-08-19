/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Set;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.springframework.security.saml2.core.OpenSamlInitializationService;

final class AuthnContextBuilder {

  static {
    OpenSamlInitializationService.initialize();
  }

  private final RequestedAuthnContextBuilder contextBuilder;
  private final AuthnContextClassRefBuilder classRefBuilder;

  public AuthnContextBuilder() {
    var builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
    this.contextBuilder =
        requireNonNull(
            (RequestedAuthnContextBuilder)
                builderFactory.getBuilder(RequestedAuthnContext.DEFAULT_ELEMENT_NAME));
    this.classRefBuilder =
        requireNonNull(
            (AuthnContextClassRefBuilder)
                builderFactory.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME));
  }

  RequestedAuthnContext build(Set<URI> classRefs) {
    var ctx = contextBuilder.buildObject();
    ctx.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
    ctx.getAuthnContextClassRefs()
        .addAll(
            classRefs.stream()
                .map(
                    uri -> {
                      var ref = classRefBuilder.buildObject();
                      ref.setURI(uri.toString());
                      return ref;
                    })
                .toList());
    return ctx;
  }
}
