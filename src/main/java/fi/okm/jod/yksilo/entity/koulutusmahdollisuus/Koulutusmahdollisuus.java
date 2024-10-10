/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity.koulutusmahdollisuus;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
public class Koulutusmahdollisuus {
  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  private KoulutusmahdollisuusTyyppi tyyppi;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 1000)
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(mappedBy = "koulutusmahdollisuus", fetch = FetchType.LAZY)
  @BatchSize(size = 100)
  private Set<KoulutusViite> koulutukset;

  @OneToMany(mappedBy = "koulutusmahdollisuus", fetch = FetchType.LAZY)
  @BatchSize(size = 100)
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "tyyppi")
  private Map<KoulutusmahdollisuusJakaumaTyyppi, KoulutusmahdollisuusJakauma> jakaumat;

  @Embedded
  @AttributeOverride(name = "minimi", column = @Column(name = "kesto_minimi"))
  @AttributeOverride(name = "mediaani", column = @Column(name = "kesto_mediaani"))
  @AttributeOverride(name = "maksimi", column = @Column(name = "kesto_maksimi"))
  private KestoJakauma kesto;

  public LocalizedString getOtsikko() {
    return LocalizedString.of(kaannos, Kaannos::otsikko);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::kuvaus);
  }

  public LocalizedString getTiivistelma() {
    return LocalizedString.of(kaannos, Kaannos::tiivistelma);
  }

  @Embeddable
  public record KestoJakauma(double minimi, double mediaani, double maksimi) {}

  @Embeddable
  public record Kaannos(
      @Column(length = Integer.MAX_VALUE) String otsikko,
      @Column(length = Integer.MAX_VALUE) String tiivistelma,
      @Column(length = Integer.MAX_VALUE) String kuvaus) {}
}
