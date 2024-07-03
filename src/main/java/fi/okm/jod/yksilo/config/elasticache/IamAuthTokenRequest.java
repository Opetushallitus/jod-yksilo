/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

public final class IamAuthTokenRequest {
  private static final Duration TOKEN_EXPIRY_DURATION_SECONDS = Duration.ofSeconds(900);

  private final SdkHttpFullRequest request;
  private final Region region;

  // Suppress deprecation warnings for the Aws4Signer class because it is same implementation as
  // in the software.amazon.awssdk.services.rds.DefaultRdsUtilities.generateAuthenticationToken.
  @SuppressWarnings("deprecation")
  private final software.amazon.awssdk.auth.signer.Aws4Signer signer = Aws4Signer.create();

  public IamAuthTokenRequest(String userId, String cacheName, Region region) {
    this.request =
        SdkHttpFullRequest.builder()
            .method(SdkHttpMethod.GET)
            .protocol("https")
            .host(cacheName)
            .encodedPath("/")
            .appendRawQueryParameter("Action", "connect")
            .appendRawQueryParameter("User", userId)
            .putRawQueryParameter("ResourceType", Collections.singletonList("ServerlessCache"))
            .build();
    this.region = region;
  }

  public String toSignedRequestUri(AwsCredentials credentials) {
    return sign(request, credentials).getUri().toString().replace("https://", "");
  }

  private SdkHttpFullRequest sign(SdkHttpFullRequest request, AwsCredentials credentials) {
    Instant expiryInstant = Instant.now().plus(TOKEN_EXPIRY_DURATION_SECONDS);
    Aws4PresignerParams signerParams =
        Aws4PresignerParams.builder()
            .signingRegion(region)
            .awsCredentials(credentials)
            .signingName("elasticache")
            .expirationTime(expiryInstant)
            .build();
    return signer.presign(request, signerParams);
  }
}
