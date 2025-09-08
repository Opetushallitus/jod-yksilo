/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.net.URI;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Ammattiryhma-table is populated every night by lambda-function. Lambda-function gets all
 * ammattiryhma-statistics from Tietoalusta and saves them to this table.
 */
@Entity
@Getter
@Immutable
public class Ammattiryhma extends JodEntity {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Long id;

  @Column(unique = true, nullable = false)
  private URI escoUri;

  @Column private Integer mediaaniPalkka;

  @Column private Integer ylinDesiiliPalkka;

  @Column private Integer alinDesiiliPalkka;
}
