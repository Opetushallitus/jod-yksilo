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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Yksilo {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Yksilo.Kaannos> kaannos = new EnumMap<>(Kieli.class);

  @Setter private Boolean tervetuloapolku;

  @Setter private Boolean lupaLuovuttaaTiedotUlkopuoliselle;

  @Setter private Boolean lupaArkistoida;

  @Setter private Boolean lupaKayttaaTekoalynKoulutukseen;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = 100)
  private Set<YksilonOsaaminen> osaamiset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Tyopaikka> tyopaikat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<KoulutusKokonaisuus> koulutusKokonaisuudet;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Toiminto> toiminnot;

  @ManyToMany private Set<Osaaminen> osaamisKiinnostukset;

  @ManyToMany private Set<Ammatti> ammattiKiinnostukset;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<YksilonSuosikki> suosikit;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<Paamaara> paamaarat;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<TapahtumaLoki> tapahtumat;

  public Yksilo(UUID uuid) {
    this.id = uuid;
    this.tervetuloapolku = false;
    this.lupaLuovuttaaTiedotUlkopuoliselle = false;
    this.lupaArkistoida = false;
    this.lupaKayttaaTekoalynKoulutukseen = false;
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

  public boolean getLupaLuovuttaaTiedotUlkopuoliselle() {
    return lupaLuovuttaaTiedotUlkopuoliselle != null && lupaLuovuttaaTiedotUlkopuoliselle;
  }

  public boolean getLupaArkistoida() {
    return lupaArkistoida != null && lupaArkistoida;
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
  @Data
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
