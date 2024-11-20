package com.technical.task.task_project.service;

import com.technical.task.task_project.security.jwt.JwtUtil;
import com.technical.task.task_project.service.dto.AuthenticationRequestDTO;
import com.technical.task.task_project.service.mapper.RoleToAuthorityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RoleToAuthorityMapper roleMapper;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAuthenticate_Success() {

        String email = "user@example.com";
        String password = "password";
        String expectedToken = "jwt-token";
        AuthenticationRequestDTO request = new AuthenticationRequestDTO(email, password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
        when(authenticationManager.authenticate(authToken)).thenReturn(mock(Authentication.class));

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        Set roleUser = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenReturn(roleUser);

        when(jwtUtil.generateToken(email, Set.of("ROLE_USER"))).thenReturn(expectedToken);


        String token = authenticationService.authenticate(request);


        assertEquals(expectedToken, token);
        verify(authenticationManager).authenticate(authToken);
        verify(userDetailsService).loadUserByUsername(email);
        verify(jwtUtil).generateToken(email, Set.of("ROLE_USER"));
    }

    @Test
    public void testAuthenticate_InvalidCredentials() {

        String email = "user@example.com";
        String password = "invalid-password";
        AuthenticationRequestDTO request = new AuthenticationRequestDTO(email, password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
        doThrow(new RuntimeException("Invalid credentials")).when(authenticationManager).authenticate(authToken);


        Exception exception = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(request));
        assertEquals("Invalid credentials", exception.getMessage());

        verify(authenticationManager).authenticate(authToken);
        verifyNoInteractions(userDetailsService, jwtUtil);
    }
}
