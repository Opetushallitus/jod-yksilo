/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Yksilo {

  @Transient private boolean isNew = false;

  @Column(updatable = false, nullable = false)
  @Id
  private UUID id;

  @NotNull
  @Column(nullable = false, unique = true)
  private String tunnus;

  @OneToMany(mappedBy = "yksilo", fetch = FetchType.LAZY)
  @BatchSize(size = 10)
  private List<Tyopaikka> tyopaikat;

  public Yksilo(UUID uuid, String user) {
    this.id = uuid;
    this.tunnus = user;
    this.isNew = true;
  }

  protected Yksilo() {
    // For JPA
  }
}
