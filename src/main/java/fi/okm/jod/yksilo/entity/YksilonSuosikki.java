/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import jakarta.persistence.CheckConstraint;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Getter
@Table(
    indexes = {@Index(columnList = "yksilo_id")},
    check =
        @CheckConstraint(
            constraint =
                "(tyomahdollisuus_id IS NULL OR koulutusmahdollisuus_id IS NULL) AND ((tyyppi = 'TYOMAHDOLLISUUS' and tyomahdollisuus_id IS NOT NULL) OR (tyyppi = 'KOULUTUSMAHDOLLISUUS' and koulutusmahdollisuus_id IS NOT NULL))"))
public class YksilonSuosikki {
  @GeneratedValue @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private Instant luotu = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ManyToOne(fetch = FetchType.LAZY)
  private Tyomahdollisuus tyomahdollisuus;

  @ManyToOne(fetch = FetchType.LAZY)
  private Koulutusmahdollisuus koulutusmahdollisuus;

  @Enumerated(EnumType.STRING)
  private SuosikkiTyyppi tyyppi;

  protected YksilonSuosikki() {
    // For JPA
  }

  public YksilonSuosikki(Yksilo yksilo, Tyomahdollisuus tyomahdollisuus) {
    this.yksilo = yksilo;
    this.tyomahdollisuus = tyomahdollisuus;
    this.tyyppi = SuosikkiTyyppi.TYOMAHDOLLISUUS;
  }

  public YksilonSuosikki(Yksilo yksilo, Koulutusmahdollisuus koulutusmahdollisuus) {
    this.yksilo = yksilo;
    this.koulutusmahdollisuus = koulutusmahdollisuus;
    this.tyyppi = SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS;
  }

  public UUID getKohdeId() {
    return switch (tyyppi) {
      case TYOMAHDOLLISUUS -> tyomahdollisuus.getId();
      case KOULUTUSMAHDOLLISUUS -> koulutusmahdollisuus.getId();
    };
  }
}
