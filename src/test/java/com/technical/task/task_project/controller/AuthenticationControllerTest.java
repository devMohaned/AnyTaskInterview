package com.technical.task.task_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.dto.AuthenticationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
public class AuthenticationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        userRepository.deleteAll();

        User testUser = User.builder()
                .id(UUID.randomUUID())
                .email("testuser@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Test User")
                .roles(Set.of(Role.DEVELOPER))
                .isActive(true)
                .build();

        userRepository.save(testUser);
    }

    @Test
    public void testCreateToken_Success() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("testuser@example.com", "password");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("eyJ"))); // Basic check for JWT format
    }

    @Test
    public void testCreateToken_InvalidCredentials() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("testuser@example.com", "wrongpassword");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateToken_UserNotFound() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("nonexistent@example.com", "password");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateToken_MissingFields() throws Exception {
        String invalidRequest = "{ \"email\": \"testuser@example.com\" }";

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print()).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateToken_emptyPassword() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("testuser@example.com", null);

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))).andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateToken_UnsupportedMediaType() throws Exception {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("testuser@example.com", "password");

        mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(objectMapper.writeValueAsString(request))).andDo(print())
                .andExpect(status().isInternalServerError());
    }

}