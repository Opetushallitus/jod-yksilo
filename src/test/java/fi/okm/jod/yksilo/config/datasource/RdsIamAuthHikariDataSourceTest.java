/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;

public class RdsIamAuthHikariDataSourceTest {

  private static final String HOST = "localhost";
  private static final Integer PORT = 5432;
  private static final String USERNAME = "user123";

  @Test
  void testGetPassword() {
    String expectedPasswordPrefix = HOST + ":" + PORT + "/?DBUser=" + USERNAME + "&Action=connect";
    RdsClient rdsClient =
        RdsClient.builder()
            .region(Region.regions().getFirst())
            .credentialsProvider(() -> AwsBasicCredentials.create("accessKey", "secretKey"))
            .build();
    String password;
    try (RdsIamAuthHikariDataSource dataSource = new RdsIamAuthHikariDataSource()) {
      dataSource.setRdsClient(rdsClient);
      dataSource.setJdbcUrl("jdbc:postgresql://" + HOST + ":" + PORT + "/testdb");
      dataSource.setUsername(USERNAME);
      password = dataSource.getPassword();
    }

    assertTrue(password.startsWith(expectedPasswordPrefix));
    assertTrue(password.contains("X-Amz-Signature"));
  }
}
