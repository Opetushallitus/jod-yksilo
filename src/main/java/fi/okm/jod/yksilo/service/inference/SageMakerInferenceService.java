/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import brave.Tracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceOverloadedException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest;
import software.amazon.awssdk.services.sagemakerruntime.model.ModelErrorException;
import software.amazon.awssdk.services.sagemakerruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.sagemakerruntime.model.SageMakerRuntimeException;
import software.amazon.awssdk.services.sagemakerruntime.model.ServiceUnavailableException;

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
  public R infer(String endpoint, T payload, ParameterizedTypeReference<R> responseType) {
    try {
      var request =
          InvokeEndpointRequest.builder()
              .endpointName(endpoint)
              .customAttributes(tracer.currentSpan().context().traceIdString())
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .body(SdkBytes.fromByteArray(objectMapper.writeValueAsBytes(payload)))
              .build();

      var response = sageMakerClient.invokeEndpoint(request);
      var javaType = objectMapper.getTypeFactory().constructType(responseType.getType());
      return objectMapper.readValue(response.body().asInputStream(), javaType);

    } catch (IOException e) {
      throw new ServiceException("Invoking SageMaker failed", e);
    } catch (ModelNotReadyException | ServiceUnavailableException e) {
      log.warn("SageMaker service unavailable: {}", e.getMessage());
      throw new fi.okm.jod.yksilo.service.ServiceUnavailableException(
          "SageMaker model not ready or service unavailable", e);
    } catch (ModelErrorException e) {
      throw new ServiceValidationException(e.originalMessage());
    } catch (SageMakerRuntimeException e) {
      if ("ThrottlingException".equals(e.awsErrorDetails().errorCode())) {
        throw new ServiceOverloadedException("SageMaker is throttling requests", e);
      }
      if ("ValidationError".equals(e.awsErrorDetails().errorCode())) {
        throw new ServiceValidationException("Invalid request", e);
      }
      throw new ServiceException("Inference failed", e);
    }
  }
}
