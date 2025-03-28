/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(
    exclude = ObservationAutoConfiguration.class /* excluded due to intermittent test failures */)
@Testcontainers
@AutoConfigureMockMvc
@Execution(ExecutionMode.CONCURRENT)
@ResourceLock("SLOW")
class ApplicationTest {

  @Autowired private MockMvc mockMvc;
  @LocalServerPort private int port;
  @Autowired ObjectMapper mapper;

  @Container @ServiceConnection
  static GenericContainer<?> redisContainer =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  @Test
  void contextLoads() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void invalidRequestShouldReturnErrorInfo() throws IOException {
    try (var socket = new Socket("localhost", port);
        var out = new PrintWriter(socket.getOutputStream());
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      // Send an invalid request (not possible using a higher-level client like RestClient)
      out.print("GET /invalid/ö HTTP/1.0\r\n");
      out.print("Connection: Close\r\n\r\n");
      out.flush();

      var statusLine = in.readLine();
      assertTrue(statusLine.startsWith("HTTP/") && statusLine.contains(" 400"));
      var body =
          in.lines().dropWhile(line -> !line.isEmpty()).skip(1).collect(Collectors.joining());

      var errorInfo = mapper.readValue(body, ErrorInfo.class);
      assertEquals(ErrorCode.INVALID_REQUEST, errorInfo.errorCode());
    }
  }

  @Test
  void shouldRequireAuthentication() {
    var restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
    var response =
        restClient
            .get()
            .uri("/yksilo/api/csrf")
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {})
            .toEntity(ErrorInfo.class);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ErrorCode.AUTHENTICATION_FAILURE, response.getBody().errorCode());
  }
}
