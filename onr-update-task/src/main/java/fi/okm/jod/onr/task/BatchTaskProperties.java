/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the batch task. */
@ConfigurationProperties(prefix = "jod.onr-task")
record BatchTaskProperties(int batchSize, long batchDelayMs) {}
