/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;

class RedisIamAuthCredentialsProviderTest {

  private static final String USER_ID = "user123";
  private static final String CACHE_NAME = "cache-123";

  @Test
  void testResolveCredentials() {
    var tokenRequest = new IamAuthTokenRequest(USER_ID, CACHE_NAME, Region.EU_WEST_1);
    var provider =
        new RedisIamAuthCredentialsProvider(
            USER_ID, tokenRequest, () -> AwsBasicCredentials.create("accessKey", "secretKey"));
    var credentials = provider.resolveCredentials().block();

    assertNotNull(credentials);
    assertEquals(USER_ID, credentials.getUsername());
    assertTrue(String.valueOf(credentials.getPassword()).contains("X-Amz-Signature"));
  }
}
