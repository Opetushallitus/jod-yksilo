/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.regions.Region;

@Slf4j
public final class IamAuthTokenRequest {
  private static final Duration TOKEN_EXPIRY_DURATION = Duration.ofSeconds(900);

  private final SdkHttpRequest request;
  private final Region region;
  private final Clock clock;

  private final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

  public IamAuthTokenRequest(String userId, String cacheName, Region region) {
    this(userId, cacheName, region, Clock.systemUTC());
  }

  // for testing
  IamAuthTokenRequest(String userId, String cacheName, Region region, Clock clock) {
    this.request =
        SdkHttpRequest.builder()
            .method(SdkHttpMethod.GET)
            .protocol("https")
            .host(cacheName)
            .encodedPath("/")
            .appendRawQueryParameter("Action", "connect")
            .appendRawQueryParameter("User", userId)
            .putRawQueryParameter("ResourceType", Collections.singletonList("ServerlessCache"))
            .build();
    this.region = region;
    this.clock = clock;
  }

  public String toSignedRequestUri(AwsCredentials credentials) {
    return sign(credentials).getUri().toString().replace("https://", "");
  }

  @SuppressWarnings("java:S3252")
  private SdkHttpRequest sign(AwsCredentials credentials) {
    return signer
        .sign(
            r ->
                r.identity(credentials)
                    .request(request)
                    .putProperty(AwsV4HttpSigner.SIGNING_CLOCK, clock)
                    .putProperty(AwsV4HttpSigner.REGION_NAME, region.toString())
                    .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "elasticache")
                    // presigned-->
                    //   - expiration duration must be set
                    //   - auth location must be set to query string
                    //   - payload signing must be enabled and the payload must be null (no payload)
                    .putProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, true)
                    .putProperty(AwsV4HttpSigner.EXPIRATION_DURATION, TOKEN_EXPIRY_DURATION)
                    .putProperty(AwsV4HttpSigner.AUTH_LOCATION, AuthLocation.QUERY_STRING)
                    .build())
        .request();
  }
}
