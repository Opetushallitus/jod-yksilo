/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceOverloadedException;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointResponse;
import software.amazon.awssdk.services.sagemakerruntime.model.ModelErrorException;
import software.amazon.awssdk.services.sagemakerruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.sagemakerruntime.model.SageMakerRuntimeException;
import software.amazon.awssdk.services.sagemakerruntime.model.ServiceUnavailableException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("cloud")
@Slf4j
public class SageMakerInferenceService<T, R> implements InferenceService<T, R> {

  private final ObjectMapper objectMapper;
  private final SageMakerRuntimeClient sageMakerClient;
  private final Tracer tracer;

  public SageMakerInferenceService(
      ObjectMapper objectMapper, SageMakerRuntimeClient sageMakerClient, Tracer tracer) {

    this.objectMapper = objectMapper;
    this.sageMakerClient = sageMakerClient;
    this.tracer = tracer;
  }

  @Override
  public R infer(
      InferenceEndpoint endpoint, T payload, ParameterizedTypeReference<R> responseType) {
    var span = tracer.nextSpan().name("sagemaker.infer").start();
    try (var _ = tracer.withSpan(span)) {
      var request =
          InvokeEndpointRequest.builder()
              .endpointName(endpoint.name())
              .inferenceComponentName(endpoint.component())
              .customAttributes(span.context().traceId())
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .body(SdkBytes.fromByteArray(objectMapper.writeValueAsBytes(payload)))
              .build();

      var response = invokeEndpoint(request);
      var javaType = objectMapper.getTypeFactory().constructType(responseType.getType());
      return objectMapper.readValue(response.body().asInputStream(), javaType);

    } catch (tools.jackson.core.JacksonException e) {
      span.error(e);
      throw new ServiceException("Invoking SageMaker failed", e);
    } catch (ModelNotReadyException | ServiceUnavailableException e) {
      log.warn("SageMaker service unavailable: {}", e.getMessage());
      span.error(e);
      throw new fi.okm.jod.yksilo.service.ServiceUnavailableException(
          "SageMaker model not ready or service unavailable", e);
    } catch (ModelErrorException e) {
      span.error(e);
      throw new ServiceException("SageMaker model error: " + e.originalMessage(), e);
    } catch (SageMakerRuntimeException e) {
      span.error(e);
      if ("ThrottlingException".equals(e.awsErrorDetails().errorCode())) {
        throw new ServiceOverloadedException("SageMaker is throttling requests", e);
      }
      throw new ServiceException("Inference failed", e);
    } finally {
      span.end();
    }
  }

  /** Invokes the endpoint, retrying once if the model container did not respond at all. */
  private InvokeEndpointResponse invokeEndpoint(InvokeEndpointRequest request) {
    try {
      return sageMakerClient.invokeEndpoint(request);
    } catch (ModelErrorException e) {
      // Original status 0 means no response from the container (e.g. it closed a reused
      // connection); the SDK does not retry model errors.
      if (e.statusCode() == 424 && Integer.valueOf(0).equals(e.originalStatusCode())) {
        log.warn("No response from SageMaker model, retrying once: {}", e.originalMessage());
        return sageMakerClient.invokeEndpoint(request);
      }
      throw e;
    }
  }
}
