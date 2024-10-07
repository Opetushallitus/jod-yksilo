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
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
// TODO: move constraint to migration tool and add koulutusmahdollisuus type when available
@Check(constraints = "tyomahdollisuus_id IS NOT NULL AND tyyppi = 'TYOMAHDOLLISUUS'")
@AllArgsConstructor
@NoArgsConstructor
public class YksilonSuosikki {
  @GeneratedValue @Id private UUID id;

  @Column(name = "luotu", nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ManyToOne
  @JoinColumn(name = "tyomahdollisuus_id", referencedColumnName = "id")
  private Tyomahdollisuus tyomahdollisuus;

  @Column(name = "tyyppi", nullable = false)
  @Enumerated(EnumType.STRING)
  private SuosikkiTyyppi tyyppi;

  public YksilonSuosikki(Yksilo yksilo, Tyomahdollisuus tyomahdollisuus) {
    this.id = UUID.randomUUID();
    this.yksilo = yksilo;
    this.tyomahdollisuus = tyomahdollisuus;
    this.createdAt = Instant.now();
    this.tyyppi = SuosikkiTyyppi.TYOMAHDOLLISUUS;
  }

  public UUID getKohdeId() {
    return switch (tyyppi) {
      case TYOMAHDOLLISUUS -> tyomahdollisuus.getId();
      case KOULUTUSMAHDOLLISUUS -> null;
    };
  }
}
