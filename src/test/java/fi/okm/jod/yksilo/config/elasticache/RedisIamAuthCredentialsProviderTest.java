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
import static org.mockito.Mockito.*;

import io.lettuce.core.RedisCredentials;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

class RedisIamAuthCredentialsProviderTest {

  private static final String USERNAME = "user123";
  private static final String PASSWORD = "password123";

  @Test
  void testResolveCredentials() {
    IamAuthTokenRequest mockIamAuthTokenRequest = mock(IamAuthTokenRequest.class);
    AwsCredentialsProvider mockAwsCredentialsProvider = mock(AwsCredentialsProvider.class);
    when(mockIamAuthTokenRequest.toSignedRequestUri(any())).thenReturn(PASSWORD);
    RedisIamAuthCredentialsProvider provider =
        new RedisIamAuthCredentialsProvider(
            USERNAME, mockIamAuthTokenRequest, mockAwsCredentialsProvider);
    Mono<RedisCredentials> credentialsMono = provider.resolveCredentials();
    RedisCredentials credentials = credentialsMono.block();
    assertNotNull(credentials);
    assertEquals(USERNAME, credentials.getUsername());
    assertEquals(PASSWORD, new String(credentials.getPassword()));
    verify(mockIamAuthTokenRequest).toSignedRequestUri(any());
    verify(mockAwsCredentialsProvider).resolveCredentials();
  }
}
