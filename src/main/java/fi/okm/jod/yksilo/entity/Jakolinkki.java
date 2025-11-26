/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static fi.okm.jod.yksilo.validation.Limits.MAX_IN_SIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
public class Jakolinkki {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  private String nimi;
  private String muistiinpano;
  private boolean kotikuntaJaettu;
  private boolean syntymavuosiJaettu;
  private boolean muuOsaaminenJaettu;
  private boolean kiinnostuksetJaettu;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ManyToMany
  @JoinTable(
      name = "jakolinkki_tyopaikat",
      schema = "yksilo",
      joinColumns = @JoinColumn(name = "jakolinkki_id"),
      inverseJoinColumns = @JoinColumn(name = "tyopaikka_id"))
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Tyopaikka> tyopaikat;

  @ManyToMany
  @JoinTable(
      name = "jakolinkki_koulutukset",
      schema = "yksilo",
      joinColumns = @JoinColumn(name = "jakolinkki_id"),
      inverseJoinColumns = @JoinColumn(name = "koulutus_kokonaisuus_id"))
  @BatchSize(size = MAX_IN_SIZE)
  private Set<KoulutusKokonaisuus> koulutukset;

  @ManyToMany
  @JoinTable(
      name = "jakolinkki_toiminnot",
      schema = "yksilo",
      joinColumns = @JoinColumn(name = "jakolinkki_id"),
      inverseJoinColumns = @JoinColumn(name = "toiminto_id"))
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Toiminto> toiminnot;

  @ManyToMany
  @JoinTable(
      name = "jakolinkki_tavoitteet",
      schema = "yksilo",
      joinColumns = @JoinColumn(name = "jakolinkki_id"),
      inverseJoinColumns = @JoinColumn(name = "tavoite_id"))
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Tavoite> tavoitteet;

  private boolean tyomahdollisuusSuosikitJaettu;
  private boolean koulutusmahdollisuusSuosikitJaettu;
}
