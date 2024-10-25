/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyEnumerated;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
public class Ammatti {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Long id;

  @Column(unique = true, nullable = false)
  private String uri;

  @Column(nullable = false)
  private String koodi;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 1000)
  private Map<Kieli, Kaannos> kaannos;

  @Embeddable
  public record Kaannos(
      @Column(length = Integer.MAX_VALUE, nullable = false) String nimi,
      @Column(length = Integer.MAX_VALUE) String kuvaus) {}

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::nimi);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::kuvaus);
  }
}
