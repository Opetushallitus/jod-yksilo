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
import jakarta.persistence.*;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Osaaminen {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Long id;

  @Column(unique = true, nullable = false)
  private URI uri;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 1000)
  private Map<Kieli, Kaannos> kaannos;

  @ManyToMany
  @JoinTable(
      name = "yksilo_osaamis_kiinnostukset",
      inverseJoinColumns =
          @JoinColumn(name = "yksilo_id", referencedColumnName = "id", columnDefinition = "uuid"),
      joinColumns =
          @JoinColumn(
              name = "osaamis_kiinnostukset_id",
              referencedColumnName = "id",
              columnDefinition = "bigint"))
  private Set<Yksilo> kiinnostuneet;

  public Osaaminen(URI uri) {
    this.uri = uri;
  }

  @Embeddable
  public record Kaannos(
      @Column(length = Integer.MAX_VALUE) String nimi,
      @Column(length = Integer.MAX_VALUE) String kuvaus) {}

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::nimi);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::kuvaus);
  }
}
