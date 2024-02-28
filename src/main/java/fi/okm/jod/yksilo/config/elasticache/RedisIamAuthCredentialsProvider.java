/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.elasticache;

import com.google.common.base.Suppliers;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@Slf4j
public class RedisIamAuthCredentialsProvider implements RedisCredentialsProvider {

  private final String username;
  private final IamAuthTokenRequest iamAuthTokenRequest;
  private final AwsCredentialsProvider awsCredentialsProvider;
  private final Supplier<String> iamAuthTokenProvider;
  private static final long TOKEN_CACHE_SECONDS = 600;

  public RedisIamAuthCredentialsProvider(
      String username,
      IamAuthTokenRequest iamAuthTokenRequest,
      AwsCredentialsProvider awsCredentialsProvider) {
    this.username = username;
    this.iamAuthTokenRequest = iamAuthTokenRequest;
    this.awsCredentialsProvider = awsCredentialsProvider;
    this.iamAuthTokenProvider =
        Suppliers.memoizeWithExpiration(
            this::getIamAuthToken, TOKEN_CACHE_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public Mono<RedisCredentials> resolveCredentials() {
    RedisCredentials redisCredentials = RedisCredentials.just(username, iamAuthTokenProvider.get());

    log.debug(
        "Using credentials: {}, {}",
        redisCredentials.getUsername(),
        String.valueOf(redisCredentials.getPassword()));

    return Mono.just(redisCredentials);
  }

  private String getIamAuthToken() {
    return iamAuthTokenRequest.toSignedRequestUri(awsCredentialsProvider.resolveCredentials());
  }
}
