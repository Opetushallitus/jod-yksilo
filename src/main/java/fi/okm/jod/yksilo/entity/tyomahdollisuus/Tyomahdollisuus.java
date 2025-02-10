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
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
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
  @MapKeyColumn(name = "tyyppi")
  private Map<TyomahdollisuusJakaumaTyyppi, TyomahdollisuusJakauma> jakaumat;

  @Column private String ammattiryhma;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TyomahdollisuusAineisto aineisto;

  @Embeddable
  public record Kaannos(
      @Column(length = Integer.MAX_VALUE) String otsikko,
      @Column(length = Integer.MAX_VALUE) String tiivistelma,
      @Column(length = Integer.MAX_VALUE) String kuvaus,
      @Column(length = Integer.MAX_VALUE) String tehtavat,
      @Column(length = Integer.MAX_VALUE) String yleisetVaatimukset) {}

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
