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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Super-luokka tässä repositoriossa oleville entityille. Tarkoituksena on, että kaikki
 * entity-luokat perisivät tulevaisuudessä tämän. Mahdollistaa yhtenäiset id:t ja sen ettei kaikille
 * entityille toteutettavaa logiikka ei tarvitse toistaa
 */
@Getter
@Setter
@ToString(callSuper = true)
@MappedSuperclass
public abstract class JodEntity {

  @Id
  @GeneratedValue
  @Column(name = "id")
  protected UUID id;

  @Column(name = "created_at", updatable = false, nullable = false)
  @CreationTimestamp
  private Instant createdAt;

  @Column(name = "modified_at")
  @UpdateTimestamp
  private Instant modifiedAt;
}
