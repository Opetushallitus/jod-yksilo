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
import jakarta.validation.constraints.NotNull;
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

  @NotNull
  @Column(nullable = false, unique = true, length = 300)
  private String tunnus;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = 10)
  private Set<YksilonOsaaminen> osaamiset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Tyopaikka> tyopaikat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<KoulutusKategoria> kategoriat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Koulutus> koulutukset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Toiminto> toiminnot;

  public Yksilo(UUID uuid, String user) {
    this.id = uuid;
    this.tunnus = user;
  }

  protected Yksilo() {
    // For JPA
  }
}
