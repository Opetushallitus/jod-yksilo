/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    indexes = {
      @Index(columnList = "yksilo_id,lahde"),
      @Index(columnList = "koulutus_id"),
      @Index(columnList = "toimenkuva_id"),
      @Index(columnList = "patevyys_id"),
    })
public class YksilonOsaaminen {
  @Getter @Id @GeneratedValue private UUID id;

  @Getter
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Yksilo yksilo;

  @Getter
  @NotNull
  @ManyToOne
  @JoinColumn(nullable = false)
  private Osaaminen osaaminen;

  @Setter
  @Enumerated(EnumType.STRING)
  @NotNull
  @Column(nullable = false)
  private OsaamisenLahdeTyyppi lahde;

  @ManyToOne(fetch = FetchType.LAZY)
  private Koulutus koulutus;

  @ManyToOne(fetch = FetchType.LAZY)
  private Toimenkuva toimenkuva;

  @ManyToOne(fetch = FetchType.LAZY)
  private Patevyys patevyys;

  protected YksilonOsaaminen() {
    // JPA
  }

  public YksilonOsaaminen(OsaamisenLahde lahde, Osaaminen osaaminen) {
    this.yksilo = requireNonNull(lahde.getYksilo());
    this.osaaminen = osaaminen;
    switch (lahde) {
      case Koulutus k -> {
        this.lahde = OsaamisenLahdeTyyppi.KOULUTUS;
        this.koulutus = k;
      }
      case Toimenkuva t -> {
        this.lahde = OsaamisenLahdeTyyppi.TOIMENKUVA;
        this.toimenkuva = t;
      }
      case Patevyys p -> {
        this.lahde = OsaamisenLahdeTyyppi.TOIMENKUVA;
        this.patevyys = p;
      }
    }
  }

  public OsaamisenLahde getLahde() {
    return switch (this.lahde) {
      case KOULUTUS -> this.koulutus;
      case TOIMENKUVA -> this.toimenkuva;
      case PATEVYYS -> this.patevyys;
    };
  }
}
