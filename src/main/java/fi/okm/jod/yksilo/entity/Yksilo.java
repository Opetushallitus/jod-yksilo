/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static fi.okm.jod.yksilo.entity.Translation.merge;
import static fi.okm.jod.yksilo.validation.Limits.MAX_IN_SIZE;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.Sukupuoli;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Yksilo extends JodEntity {
  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = MAX_IN_SIZE)
  private Map<Kieli, Yksilo.Kaannos> kaannos = new EnumMap<>(Kieli.class);

  @Setter private Boolean tervetuloapolku;

  @Setter private Boolean lupaLuovuttaaTiedotUlkopuoliselle;

  @Setter private Boolean lupaKayttaaTekoalynKoulutukseen;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = MAX_IN_SIZE)
  private Set<YksilonOsaaminen> osaamiset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Tyopaikka> tyopaikat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<KoulutusKokonaisuus> koulutusKokonaisuudet;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Toiminto> toiminnot;

  @ManyToMany
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Osaaminen> osaamisKiinnostukset;

  @ManyToMany
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Ammatti> ammattiKiinnostukset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = MAX_IN_SIZE)
  private Set<YksilonSuosikki> suosikit;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = MAX_IN_SIZE)
  private Set<Tavoite> tavoitteet;

  // Demographic information
  @Setter private Integer syntymavuosi;

  @Setter
  @Enumerated(EnumType.STRING)
  private Sukupuoli sukupuoli;

  @Setter private String kotikunta;
  @Setter private String aidinkieli;

  @Setter
  @Enumerated(EnumType.STRING)
  private Kieli valittuKieli;

  public Yksilo(UUID uuid) {
    this.id = uuid;
    this.tervetuloapolku = false;
    this.lupaLuovuttaaTiedotUlkopuoliselle = false;
    this.lupaKayttaaTekoalynKoulutukseen = false;
    this.osaamiset = new HashSet<>();
    this.tyopaikat = new HashSet<>();
    this.koulutusKokonaisuudet = new HashSet<>();
    this.toiminnot = new HashSet<>();
    this.osaamisKiinnostukset = new HashSet<>();
    this.ammattiKiinnostukset = new HashSet<>();
    this.suosikit = new HashSet<>();
    this.tavoitteet = new HashSet<>();
  }

  protected Yksilo() {
    // For JPA
  }

  public void updated() {
    this.muokattu = Instant.now();
  }

  public boolean getTervetuloapolku() {
    return tervetuloapolku != null && tervetuloapolku;
  }

  public boolean getLupaLuovuttaaTiedotUlkopuoliselle() {
    return lupaLuovuttaaTiedotUlkopuoliselle != null && lupaLuovuttaaTiedotUlkopuoliselle;
  }

  public boolean getLupaKayttaaTekoalynKoulutukseen() {
    return lupaKayttaaTekoalynKoulutukseen != null && lupaKayttaaTekoalynKoulutukseen;
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

  public LocalizedString getMuuOsaaminenVapaateksti() {
    return LocalizedString.of(kaannos, Kaannos::getMuuOsaaminenVapaateksti);
  }

  public void setMuuOsaaminenVapaateksti(LocalizedString muuOsaaminenVapaateksti) {
    merge(muuOsaaminenVapaateksti, kaannos, Kaannos::new, Kaannos::setMuuOsaaminenVapaateksti);
  }

  public LocalizedString getOsaamisKiinnostuksetVapaateksti() {
    return LocalizedString.of(kaannos, Kaannos::getOsaamisKiinnostuksetVapaateksti);
  }

  public void setOsaamisKiinnostuksetVapaateksti(LocalizedString osaamisKiinnostuksetVapaateksti) {
    merge(
        osaamisKiinnostuksetVapaateksti,
        kaannos,
        Kaannos::new,
        Kaannos::setOsaamisKiinnostuksetVapaateksti);
  }

  @Embeddable
  @Getter
  @Setter
  @EqualsAndHashCode
  static class Kaannos implements Translation {
    @Column(length = Integer.MAX_VALUE)
    String muuOsaaminenVapaateksti;

    @Column(length = Integer.MAX_VALUE)
    String osaamisKiinnostuksetVapaateksti;

    public boolean isEmpty() {
      return muuOsaaminenVapaateksti == null && osaamisKiinnostuksetVapaateksti == null;
    }
  }
}
