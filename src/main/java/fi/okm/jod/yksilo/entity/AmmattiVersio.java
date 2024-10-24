/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;

/**
 * Singleton entity for storing the current version of the Ammatti (occupation classification) data.
 */
@Entity
@Immutable
public class AmmattiVersio {
  @Id
  @Column(columnDefinition = "int generated always as (1) stored")
  private int id;

  @Column(nullable = false)
  private long versio;
}
