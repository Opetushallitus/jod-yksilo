/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;

@Slf4j
class IamAuthTokenRequestTest {

  private static final String USERNAME = "user123";
  private static final String CACHE_NAME = "my-cache";
  private static final Clock clock =
      Clock.fixed(Instant.parse("2024-08-02T12:11:50Z"), ZoneOffset.UTC);

  @Test
  void testToSignedRequestUri() {
    var testAwsCredentials = AwsBasicCredentials.create("accessKey", "secretKey");
    String expectedUriPrefix =
        CACHE_NAME + "/?Action=connect&User=" + USERNAME + "&ResourceType=ServerlessCache";
    IamAuthTokenRequest request =
        new IamAuthTokenRequest(USERNAME, CACHE_NAME, Region.EU_WEST_1, clock);
    String signedUri = request.toSignedRequestUri(testAwsCredentials);

    log.info("Signed URI: {}", signedUri);
    assertThat(signedUri)
        .startsWith(expectedUriPrefix)
        .contains(
            "X-Amz-Signature=5dda5db919b1337db6596b2a608924bc10197fb39289b933b60bdb114d5a70dc");
  }
}
