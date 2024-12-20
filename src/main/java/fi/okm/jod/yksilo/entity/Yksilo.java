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
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Yksilo {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = 10)
  private Set<YksilonOsaaminen> osaamiset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Tyopaikka> tyopaikat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<KoulutusKokonaisuus> koulutusKokonaisuudet;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Toiminto> toiminnot;

  @OneToMany(fetch = FetchType.LAZY)
  private Set<Osaaminen> osaamisKiinnostukset = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY)
  private Set<Ammatti> ammattiKiinnostukset = new HashSet<>();

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<YksilonSuosikki> suosikit;

  public Yksilo(UUID uuid) {
    this.id = uuid;
  }

  protected Yksilo() {
    // For JPA
  }

  public void setOsaamisKiinnostukset(Collection<Osaaminen> entities) {
    osaamisKiinnostukset.clear();
    osaamisKiinnostukset.addAll(entities);
  }

  public void setAmmattiKiinnostukset(Collection<Ammatti> entities) {
    ammattiKiinnostukset.clear();
    ammattiKiinnostukset.addAll(entities);
  }
}
