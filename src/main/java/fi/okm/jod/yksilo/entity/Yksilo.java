/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Yksilo {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Setter private Boolean tervetuloapolku;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = 100)
  private Set<YksilonOsaaminen> osaamiset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Tyopaikka> tyopaikat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<KoulutusKokonaisuus> koulutusKokonaisuudet;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Toiminto> toiminnot;

  @OneToMany(fetch = FetchType.LAZY)
  private Set<Osaaminen> osaamisKiinnostukset;

  @OneToMany(fetch = FetchType.LAZY)
  private Set<Ammatti> ammattiKiinnostukset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<YksilonSuosikki> suosikit;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Paamaara> paamaarat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<TapahtumaLoki> tapahtumat;

  public Yksilo(UUID uuid) {
    this.id = uuid;
    this.tervetuloapolku = false;
    this.osaamiset = new HashSet<>();
    this.tyopaikat = new HashSet<>();
    this.koulutusKokonaisuudet = new HashSet<>();
    this.toiminnot = new HashSet<>();
    this.osaamisKiinnostukset = new HashSet<>();
    this.ammattiKiinnostukset = new HashSet<>();
    this.suosikit = new HashSet<>();
    this.paamaarat = new HashSet<>();
    this.tapahtumat = new HashSet<>();
  }

  protected Yksilo() {
    // For JPA
  }

  public boolean getTervetuloapolku() {
    return tervetuloapolku != null && tervetuloapolku;
  }

  public void setOsaamisKiinnostukset(Collection<Osaaminen> entities) {
    if (osaamisKiinnostukset == null) {
      osaamisKiinnostukset = new HashSet<>();
    }
    osaamisKiinnostukset.clear();
    osaamisKiinnostukset.addAll(entities);
  }

  public void setAmmattiKiinnostukset(Collection<Ammatti> entities) {
    if (ammattiKiinnostukset == null) {
      ammattiKiinnostukset = new HashSet<>();
    }
    ammattiKiinnostukset.clear();
    ammattiKiinnostukset.addAll(entities);
  }
}
