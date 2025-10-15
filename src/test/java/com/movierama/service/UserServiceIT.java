package com.movierama.service;

import com.movierama.dto.UserRegistrationDto;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class UserServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private final UserService userService;

    @Test
    @DisplayName("registerUser persists and can be loaded via username/email")
    void register_and_load() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("secret123");
        dto.setFirstName("Alice");
        dto.setLastName("Smith");

        var saved = userService.registerUser(dto);
        assertThat(saved.getId()).isNotNull();

        var loaded = userService.loadUserByUsername("alice");
        assertThat(loaded.getUsername()).isEqualTo("alice");

        var loadedByEmail = userService.loadUserByUsername("alice@example.com");
        assertThat(loadedByEmail.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("findByUsername throws when missing")
    void find_missing() {
        assertThatThrownBy(() -> userService.findByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
