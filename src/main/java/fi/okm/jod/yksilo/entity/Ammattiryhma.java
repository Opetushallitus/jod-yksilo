/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import com.fasterxml.jackson.databind.JsonNode; // Hibernate 7 still uses Jackson 2
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Ammattiryhma-table is populated every night by lambda-function. Lambda-function gets all
 * ammattiryhma-statistics from Tietoalusta and saves them to this table.
 */
@Entity
@Getter
@Immutable
public class Ammattiryhma extends JodEntity {

  @Id
  @Column(unique = true, nullable = false)
  private String escoUri;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private JsonNode data;

  public String getKohtaanto() {
    return data.path("kohtaanto").optional("tyyppi").map(JsonNode::asText).orElse(null);
  }

  public Integer getTyollistenMaara() {
    return data.optional("tyollisetKokoMaa").map(JsonNode::asInt).orElse(null);
  }

  public Integer getMediaaniPalkka() {
    return getPropertyFromPalkkaus("mediaani");
  }

  public Integer getYlinDesiiliPalkka() {
    return getPropertyFromPalkkaus("ylinDesiili");
  }

  public Integer getAlinDesiiliPalkka() {
    return getPropertyFromPalkkaus("alinDesiili");
  }

  public Integer getPropertyFromPalkkaus(String propertyName) {
    return data.path("palkkaus").optional(propertyName).map(JsonNode::asInt).orElse(null);
  }
}
