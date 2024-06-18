/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.validation;

public final class Limits {
  public static final int TYOPAIKKA = 10_0000;
  public static final int TOIMENKUVA_PER_TYOPAIKKA = 1_000;
  public static final int KOULUTUS = 10_000;
  public static final int TOIMINTO = 1_000;
  public static final int PATEVYYS_PER_TOIMINTO = 1_000;

  private Limits() {}
}
