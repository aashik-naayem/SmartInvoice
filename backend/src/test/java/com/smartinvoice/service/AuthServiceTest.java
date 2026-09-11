package com.smartinvoice.service;

import com.smartinvoice.dto.LoginRequest;
import com.smartinvoice.dto.LoginResponse;
import com.smartinvoice.dto.RegisterRequest;
import com.smartinvoice.dto.RegisterResponse;
import com.smartinvoice.entity.User;
import com.smartinvoice.enums.Role;
import com.smartinvoice.exception.EmailAlreadyExistsException;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the two things a broken AuthService would break silently in production: a duplicate
 * email slipping through, and a bad password still returning a token.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesNewUserWithEncodedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("Ashik Nayem", "ashik@example.com", "password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("ashik@example.com");
        assertThat(response.getFullName()).isEqualTo("Ashik Nayem");
        assertThat(response.getRole()).isEqualTo(Role.USER);

        // The password that actually got persisted must be the encoded one, never the raw one.
        verify(userRepository).save(argThatPasswordIsEncoded());
    }

    @Test
    void register_rejectsDuplicateEmailWithoutTouchingPasswordEncoderOrSave() {
        RegisterRequest request = new RegisterRequest("Ashik Nayem", "ashik@example.com", "password123");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("ashik@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .fullName("Ashik Nayem")
                .email("ashik@example.com")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("ashik@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("ashik@example.com", Role.USER)).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_propagatesBadCredentialsWithoutIssuingAToken() {
        LoginRequest request = new LoginRequest("ashik@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString(), any());
    }

    @Test
    void login_failsCleanlyIfAuthenticatedUserSomehowMissingFromDb() {
        // Defensive case: authentication succeeded but the user row is gone (e.g. deleted mid-session).
        LoginRequest request = new LoginRequest("ghost@example.com", "password123");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(UsernameNotFoundException.class);
    }

    private User argThatPasswordIsEncoded() {
        return org.mockito.ArgumentMatchers.argThat(user -> "hashed-password".equals(user.getPassword()));
    }
}
