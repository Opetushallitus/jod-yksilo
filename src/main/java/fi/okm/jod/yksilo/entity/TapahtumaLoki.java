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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Table(indexes = @Index(columnList = "yksilo_id", name = "ix_tapahtuma_loki_yksilo_fk"))
public class TapahtumaLoki {
  @Id @GeneratedValue long id;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false, updatable = false)
  Yksilo yksilo;

  @Column(nullable = false, updatable = false)
  Instant luotu = Instant.now();

  @Column(nullable = false)
  Instant muokattu = Instant.now();

  @Column(nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  Tapahtuma tapahtuma;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Setter
  Tila tila;

  @Setter String kuvaus;

  @Column(nullable = false, unique = true, updatable = false)
  UUID tunniste;

  protected TapahtumaLoki() {}

  public TapahtumaLoki(Yksilo yksilo, UUID tunniste, Tapahtuma tapahtuma, Tila tila) {
    this.yksilo = yksilo;
    this.tapahtuma = tapahtuma;
    this.tila = tila;
    this.tunniste = tunniste;
  }

  @PreUpdate
  public void preUpdate() {
    muokattu = Instant.now();
  }

  public enum Tapahtuma {
    TMT_VIENTI
  }

  public enum Tila {
    KESKEN,
    VALMIS,
    VIRHE
  }
}
