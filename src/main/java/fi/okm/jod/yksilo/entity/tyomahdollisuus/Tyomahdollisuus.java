/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity.tyomahdollisuus;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.entity.Ammattiryhma;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
public class Tyomahdollisuus {
  @Id private UUID id;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 1000)
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(mappedBy = "tyomahdollisuus", fetch = FetchType.LAZY)
  @BatchSize(size = 100)
  @MapKeyEnumerated(EnumType.STRING)
  @MapKey(name = "tyyppi")
  private Map<TyomahdollisuusJakaumaTyyppi, TyomahdollisuusJakauma> jakaumat;

  @ManyToOne
  @JoinColumn(name = "ammattiryhma")
  private Ammattiryhma ammattiryhma;

  // ammattiryhma column does not necessarily have counterpart in Ammattiryhma-table
  // thats why we need to get the escoUri from here
  @Column(name = "ammattiryhma", insertable = false, updatable = false)
  private URI ammattiryhmaUri;

  @Enumerated(EnumType.STRING)
  @Column
  private TyomahdollisuusAineisto aineisto;

  @Column(columnDefinition = "boolean default true")
  private boolean aktiivinen = true;

  @Embeddable
  public record Kaannos(
      @Column(length = Integer.MAX_VALUE) String otsikko,
      @Column(length = Integer.MAX_VALUE) String tiivistelma,
      @Column(length = Integer.MAX_VALUE) String kuvaus,
      @Column(length = Integer.MAX_VALUE) String tehtavat,
      @Column(length = Integer.MAX_VALUE) String yleisetVaatimukset) {}

  // TODO: Remove this after the migration to the new data model is complete. Set nullable to false.
  public TyomahdollisuusAineisto getAineisto() {
    return aineisto == null ? TyomahdollisuusAineisto.TMT : aineisto;
  }

  public LocalizedString getOtsikko() {
    return LocalizedString.of(kaannos, Kaannos::otsikko);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::kuvaus);
  }

  public LocalizedString getTiivistelma() {
    return LocalizedString.of(kaannos, Kaannos::tiivistelma);
  }

  public LocalizedString getTehtavat() {
    return LocalizedString.of(kaannos, Kaannos::tehtavat);
  }

  public LocalizedString getYleisetVaatimukset() {
    return LocalizedString.of(kaannos, Kaannos::yleisetVaatimukset);
  }
}
