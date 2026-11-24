package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.model.ApplicationUser;
import com.vanokal0.warehouse_backend.repositories.ApplicationUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationUserServiceTest {

    @Mock
    private ApplicationUserRepository userRepository;

    @InjectMocks
    private ApplicationUserService service;

    private ApplicationUser user;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setUsername("jsmith");
        user.setPassword("secret");
    }

    @Test
    void authenticate_returnsUser_whenPasswordMatches() {
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        assertThat(service.authenticate("jsmith", "secret")).containsSame(user);
    }

    @Test
    void authenticate_returnsEmpty_whenPasswordDoesNotMatch() {
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        assertThat(service.authenticate("jsmith", "wrong")).isEmpty();
    }

    @Test
    void authenticate_isCaseSensitiveForPassword() {
        when(userRepository.findByUsername("jsmith")).thenReturn(Optional.of(user));

        assertThat(service.authenticate("jsmith", "SECRET")).isEmpty();
    }

    @Test
    void authenticate_returnsEmpty_whenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.authenticate("ghost", "secret")).isEmpty();
    }
}
