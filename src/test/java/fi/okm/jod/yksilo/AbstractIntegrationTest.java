package fi.okm.jod.yksilo;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Add the following code (remove * in the front of the code) to the implemented class as we want test classes
 * to run own container instance not to share with another processes as it will cause issues when running in parallel:
 * <p>
 * ------CODE SNIPPET START------
 * @Container @ServiceConnection
 * private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
 *     TestUtil.createPostgresSQLContainer();
 *
 * @Container @ServiceConnection
 * private static GenericContainer<?> REDIS_CONTAINER =
 *     TestUtil.createRedisContainer();
 * ------CODE SNIPPET END------
 */
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public interface AbstractIntegrationTest {
}
