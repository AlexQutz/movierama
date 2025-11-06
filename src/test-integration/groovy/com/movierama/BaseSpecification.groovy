package com.movierama

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * Base test specification for integration tests that need:
 * - PostgreSQL database
 * - Redis
 * - WireMock server for HTTP stubbing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration
abstract class BaseSpecification extends Specification {

    // ---------- Shared Testcontainers ----------
    @Shared
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("movierama_test")
            .withUsername("test")
            .withPassword("test")

    @Shared
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())

    // ---------- Dynamic Properties ----------
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        postgres.start()
        redis.start()

        registry.add("spring.datasource.url") { postgres.jdbcUrl }
        registry.add("spring.datasource.username") { postgres.username }
        registry.add("spring.datasource.password") { postgres.password }

        registry.add("spring.data.redis.host") { redis.host }
        registry.add("spring.data.redis.port") { redis.firstMappedPort }

    }

    // ---------- Autowired beans ----------
    @Autowired
    ApplicationContext context

    @Autowired
    JdbcTemplate jdbcTemplate

    @Autowired
    StringRedisTemplate redisTemplate

    // ---------- Polling utilities ----------
    PollingConditions pollingConditions = new PollingConditions(timeout: 10, delay: 1)

    // ---------- Utility methods ----------
    def clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE some_table RESTART IDENTITY CASCADE;")
    }

    def clearRedis() {
        redisTemplate.connectionFactory.connection.flushAll()
    }

    def assertCollectionEquals(Collection<?> a, Collection<?> b) {
        a.size() == b.size() && a.containsAll(b) && b.containsAll(a)
    }

    def getWireMockUrl(String path = "") {
        return "${wiremock.baseUrl}${path.startsWith('/') ? path : '/' + path}"
    }

    def cleanupSpec() {
        redis.stop()
        postgres.stop()
    }
}

