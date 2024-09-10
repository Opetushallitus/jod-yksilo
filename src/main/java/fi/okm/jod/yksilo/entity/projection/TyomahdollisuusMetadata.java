/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity.projection;

import java.util.UUID;

// TODO: Change externalId to UUID when clustering id is changed
public record TyomahdollisuusMetadata(UUID id, String externalId, String otsikko) {}
