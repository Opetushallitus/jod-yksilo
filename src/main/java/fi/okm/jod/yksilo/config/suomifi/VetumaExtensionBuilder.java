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

import fi.okm.jod.yksilo.domain.Kieli;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import lombok.Getter;
import lombok.Setter;
import net.shibboleth.shared.xml.ElementSupport;
import org.opensaml.core.xml.AbstractXMLObject;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.AbstractXMLObjectMarshaller;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.impl.ExtensionsBuilder;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.w3c.dom.Element;

/**
 * Extension for specifying Suomi.fi e-Identification user interface language.
 *
 * <p>See <a
 * href="https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/59116c3014bbb10001966f70">Tekninen
 * rajapintakuvaus</a>
 *
 * <p>Example:
 *
 * <pre>{@code
 * <saml2p:Extensions>
 *   <vetuma xmlns="urn:vetuma:SAML:2.0:extensions">
 *     <LG>fi</LG>
 *   </vetuma>
 * </saml2p:Extensions>
 * }</pre>
 */
class VetumaExtensionBuilder {

  public static final String VETUMA_NS = "urn:vetuma:SAML:2.0:extensions";
  public static final QName VETUMA_QNAME = new QName(VETUMA_NS, "vetuma");
  public static final QName LG_QNAME = new QName(VETUMA_NS, "LG");

  private final ExtensionsBuilder extensionsBuilder;

  static {
    OpenSamlInitializationService.initialize();
  }

  VetumaExtensionBuilder() {
    extensionsBuilder =
        requireNonNull(
            (ExtensionsBuilder)
                XMLObjectProviderRegistrySupport.getBuilderFactory()
                    .getBuilder(Extensions.DEFAULT_ELEMENT_NAME));

    var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
    var marshaller = new Marshaller();
    marshallerFactory.registerMarshaller(VETUMA_QNAME, marshaller);
    marshallerFactory.registerMarshaller(LG_QNAME, marshaller);
  }

  Extensions build(Kieli kieli) {
    var extensions = extensionsBuilder.buildObject();
    extensions.getUnknownXMLObjects().add(new Vetuma(kieli.toString()));
    return extensions;
  }

  static class Vetuma extends AbstractXMLObject {

    private final LG lg;

    private Vetuma(String lang) {
      super(VETUMA_QNAME.getNamespaceURI(), VETUMA_QNAME.getLocalPart(), VETUMA_QNAME.getPrefix());
      lg = new LG(lang);
      lg.setParent(this);
    }

    @Override
    public List<XMLObject> getOrderedChildren() {
      return List.of(lg);
    }
  }

  @Setter
  @Getter
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private static class LG extends AbstractXMLObject {
    private final String lang;

    private LG(String lang) {
      super(LG_QNAME.getNamespaceURI(), LG_QNAME.getLocalPart(), LG_QNAME.getPrefix());
      this.lang = lang;
    }

    @Override
    public List<XMLObject> getOrderedChildren() {
      return null;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }
  }

  private static class Marshaller extends AbstractXMLObjectMarshaller {
    @Override
    protected void marshallElementContent(
        @Nonnull XMLObject xmlObject, @Nonnull Element domElement) {
      if (xmlObject instanceof LG lg) {
        ElementSupport.appendTextContent(domElement, lg.getLang());
      }
    }
  }
}
