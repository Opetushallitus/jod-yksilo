/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;

class IamAuthTokenRequestTest {

  private static final String USERNAME = "user123";
  private static final String CACHE_NAME = "my-cache";

  @Test
  void testToSignedRequestUri() {
    var testAwsCredentials = AwsBasicCredentials.create("accessKey", "secretKey");
    String expectedUriPrefix =
        CACHE_NAME + "/?Action=connect&User=" + USERNAME + "&ResourceType=ServerlessCache";
    IamAuthTokenRequest request = new IamAuthTokenRequest(USERNAME, CACHE_NAME, Region.EU_WEST_1);
    String signedUri = request.toSignedRequestUri(testAwsCredentials);
    assertTrue(signedUri.startsWith(expectedUriPrefix));
    assertTrue(signedUri.contains("X-Amz-Signature"));
  }
}
