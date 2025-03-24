package fi.okm.jod.yksilo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * Add the following code (remove * in the front of the code) to the extended class as we want test classes (extends this)
 * to run own instance of a database and not shared with another process as it will cause issues when running in parallel:
 * <p>
 * ------CODE SNIPPET START------
 * @Container @ServiceConnection
 * private static final PostgreSQLContainer<?> postgresContainer =
 *     TestUtil.createPostgresSQLContainer();
 *
 * @Container @ServiceConnection
 * private static GenericContainer<?> redisContainer =
 *     TestUtil.createRedisContainer();
 * ------CODE SNIPPET END------
 */
@SqlConfig(separator = ";;;")
@Sql(scripts = {"/data/mock-tunnistus.sql", "/schema.sql"})
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract public class AbstractIntegrationTest {

  @Autowired
  protected MockMvc mockMvc;
}
