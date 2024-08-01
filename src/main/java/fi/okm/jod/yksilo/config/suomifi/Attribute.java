/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import lombok.Getter;

// https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/590ad07b14bbb10001966f50
@Getter
public enum Attribute {
  // Suomi.fi National attributes
  NATIONAL_IDENTIFICATION_NUMBER("urn:oid:1.2.246.21"),
  GIVEN_NAME("urn:oid:2.5.4.42"),
  SN("urn:oid:2.5.4.4"),
  CN("urn:oid:2.5.4.3"),
  DISPLAY_NAME("urn:oid:2.16.840.1.113730.3.1.241"),
  VAKINAINEN_ULKOMAINEN_LAHIOSOITE("urn:oid:1.2.246.517.2002.2.11"),
  VAKINAINEN_ULKOMAINEN_LAHIOSOITE_PAIKKAKUNTA_JA_VALTIO_S("urn:oid:1.2.246.517.2002.2.12"),
  VAKINAINEN_ULKOMAINEN_LAHIOSOITE_PAIKKAKUNTA_JA_VALTIO_R("urn:oid:1.2.246.517.2002.2.13"),
  VAKINAINEN_ULKOMAINEN_LAHIOSOITE_PAIKKAKUNTA_JA_VALTIO_SELVAKIELINEN(
      "urn:oid:1.2.246.517.2002.2.14"),
  VAKINAINEN_ULKOMAINEN_LAHIOSOITE_VALTIOKOODI_3("urn:oid:1.2.246.517.2002.2.15"),
  KOTIKUNTA_KUNTANUMERO("urn:oid:1.2.246.517.2002.2.18"),
  KOTIKUNTA_KUNTA_S("urn:oid:1.2.246.517.2002.2.19"),
  KOTIKUNTA_KUNTA_R("urn:oid:1.2.246.517.2002.2.20"),
  VAKINAINEN_KOTIMAINEN_LAHIOSOITE_S("urn:oid:1.2.246.517.2002.2.4"),
  VAKINAINEN_KOTIMAINEN_LAHIOSOITE_R("urn:oid:1.2.246.517.2002.2.5"),
  VAKINAINEN_KOTIMAINEN_LAHIOSOITE_POSTINUMERO("urn:oid:1.2.246.517.2002.2.6"),
  VAKINAINEN_KOTIMAINEN_LAHIOSOITE_POSTITOIMIPAIKKA_S("urn:oid:1.2.246.517.2002.2.7"),
  VAKINAINEN_KOTIMAINEN_LAHIOSOITE_POSTITOIMIPAIKKA_R("urn:oid:1.2.246.517.2002.2.8"),
  MAIL("urn:oid:0.9.2342.19200300.100.1.3"),
  TURVAKIELTO_TIETO("urn:oid:1.2.246.517.2002.2.27"),

  // eIDAS attributes
  PERSON_IDENTIFIER("http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier"),
  FIRST_NAME("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName"),
  FAMILY_NAME("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName"),
  DATE_OF_BIRTH("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth");

  private final String uri;

  Attribute(String uri) {
    this.uri = uri;
  }
}
