/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;

import java.util.UUID;

/**
 * A suggestion for a path step.
 *
 * @param mahdollisuusId mahdollisuus id
 * @param pisteet score or how good the match is (0-1)
 * @param osaamisia How many missing skills are present in the Mahdollisuus
 */
public record PolunVaiheEhdotusDto(UUID mahdollisuusId, KoulutusmahdollisuusTyyppi tyyppi, double pisteet, long osaamisia) {}
