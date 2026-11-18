/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fi.okm.jod.yksilo.service.ServiceException;
import io.micrometer.tracing.Tracer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointResponse;
import software.amazon.awssdk.services.sagemakerruntime.model.ModelErrorException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class SageMakerInferenceServiceTest {

  private static final InferenceEndpoint ENDPOINT = InferenceEndpoint.of("test-endpoint");
  private static final ParameterizedTypeReference<List<String>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @Test
  void succeedsWhenModelRespondsAfterNoResponse() {
    var client = new ScriptedClient(noResponse(), success("[\"ok\"]"));

    var result = service(client).infer(ENDPOINT, Map.of(), RESPONSE_TYPE);

    assertThat(result).containsExactly("ok");
  }

  @Test
  void failsWhenModelDoesNotRespondTwice() {
    var client = new ScriptedClient(noResponse(), noResponse(), success("[\"ok\"]"));

    assertThatThrownBy(() -> service(client).infer(ENDPOINT, Map.of(), RESPONSE_TYPE))
        .isInstanceOf(ServiceException.class);
    assertThat(client.invocations).isEqualTo(2);
  }

  @Test
  void doesNotRetryErrorResponseFromModel() {
    var client = new ScriptedClient(modelError(500), success("[\"ok\"]"));

    assertThatThrownBy(() -> service(client).infer(ENDPOINT, Map.of(), RESPONSE_TYPE))
        .isInstanceOf(ServiceException.class);
    assertThat(client.invocations).isEqualTo(1);
  }

  private SageMakerInferenceService<Map<String, String>, List<String>> service(
      SageMakerRuntimeClient client) {
    return new SageMakerInferenceService<>(objectMapper, client, Tracer.NOOP);
  }

  private static Object noResponse() {
    return modelError(0);
  }

  private static Object modelError(int originalStatusCode) {
    return ModelErrorException.builder()
        .statusCode(424)
        .originalStatusCode(originalStatusCode)
        .message("model error")
        .build();
  }

  private static Object success(String body) {
    return InvokeEndpointResponse.builder().body(SdkBytes.fromUtf8String(body)).build();
  }

  /** Returns the given responses (or throws the given exceptions) in order. */
  private static class ScriptedClient implements SageMakerRuntimeClient {
    private final Deque<Object> outcomes;
    int invocations;

    ScriptedClient(Object... outcomes) {
      this.outcomes = new ArrayDeque<>(List.of(outcomes));
    }

    @Override
    public InvokeEndpointResponse invokeEndpoint(InvokeEndpointRequest request) {
      invocations++;
      var outcome = outcomes.removeFirst();
      if (outcome instanceof RuntimeException e) {
        throw e;
      }
      return (InvokeEndpointResponse) outcome;
    }

    @Override
    public String serviceName() {
      return SERVICE_NAME;
    }

    @Override
    public void close() {}
  }
}
