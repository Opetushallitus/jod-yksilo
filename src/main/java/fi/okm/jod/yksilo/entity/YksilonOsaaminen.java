/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
public class YksilonOsaaminen {

  @Id @GeneratedValue private UUID id;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @NotNull
  @JoinColumn(nullable = false)
  private Yksilo yksilo;

  @Getter
  @ManyToOne
  @NotNull
  @JoinColumn(nullable = false)
  private Osaaminen osaaminen;

  @Setter
  @Enumerated(EnumType.STRING)
  @NotNull
  @Column(nullable = false)
  private OsaamisenLahdeTyyppi lahde;

  @ManyToOne private Koulutus koulutus;

  @ManyToOne private Toimenkuva toimenkuva;

  protected YksilonOsaaminen() {
    // JPA
  }

  public YksilonOsaaminen(Yksilo yksilo, Osaaminen osaaminen) {
    this.yksilo = yksilo;
    this.osaaminen = osaaminen;
  }

  public void setKoulutus(Koulutus koulutus) {
    this.lahde = OsaamisenLahdeTyyppi.KOULUTUS;
    this.koulutus = koulutus;
    this.toimenkuva = null;
  }

  public void setToimenkuva(Toimenkuva toimenkuva) {
    this.lahde = OsaamisenLahdeTyyppi.TOIMENKUVA;
    this.toimenkuva = toimenkuva;
    this.koulutus = null;
  }
}
