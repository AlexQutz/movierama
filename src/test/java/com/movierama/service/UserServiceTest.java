package com.movierama.service;

import com.movierama.dto.UserRegistrationDto;
import com.movierama.entity.User;
import com.movierama.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("loadUserByUsername returns user by username or email")
    void loadUserByUsername_ok() {
        User u = new User();
        u.setId(1L);
        u.setUsername("jdoe");
        u.setEmail("j@d.com");
        when(userRepository.findByUsernameOrEmail("jdoe", "jdoe")).thenReturn(Optional.of(u));

        assertThat(userService.loadUserByUsername("jdoe")).isEqualTo(u);
        verify(userRepository).findByUsernameOrEmail("jdoe", "jdoe");
    }

    @Test
    @DisplayName("loadUserByUsername throws when missing")
    void loadUserByUsername_missing() {
        when(userRepository.findByUsernameOrEmail("nope", "nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    @DisplayName("registerUser saves encoded password and default role")
    void registerUser_ok() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("jdoe");
        dto.setEmail("j@d.com");
        dto.setPassword("plain");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("j@d.com")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.registerUser(dto);
        assertThat(saved.getPassword()).isEqualTo("ENC");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUser rejects duplicate username")
    void registerUser_duplicateUsername() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("jdoe");
        dto.setEmail("j@d.com");
        dto.setPassword("plain");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerUser rejects duplicate email")
    void registerUser_duplicateEmail() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("jdoe");
        dto.setEmail("j@d.com");
        dto.setPassword("plain");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("j@d.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUsername returns when present, else throws")
    void findByUsername_behavior() {
        User u = new User();
        u.setUsername("jdoe");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(u));
        assertThat(userService.findByUsername("jdoe")).isEqualTo(u);

        when(userRepository.findByUsername("absent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByUsername("absent")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("findById returns when present, else throws")
    void findById_behavior() {
        User u = new User();
        u.setId(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(u));
        assertThat(userService.findById(9L)).isEqualTo(u);

        when(userRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findById(8L)).isInstanceOf(RuntimeException.class);
    }
}
