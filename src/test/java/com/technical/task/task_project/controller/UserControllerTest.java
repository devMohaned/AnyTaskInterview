package com.technical.task.task_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.repository.HistoryRepository;
import com.technical.task.task_project.repository.NotificationRepository;
import com.technical.task.task_project.repository.TaskRepository;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.security.jwt.JwtUtil;
import com.technical.task.task_project.service.dto.UserDTO;
import com.technical.task.task_project.service.dto.UserRegistrationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
public class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        notificationRepository.deleteAll();
        historyRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Admin User")
                .roles(Set.of(Role.ADMIN, Role.SCRUM))
                .isActive(true)
                .build();
        userRepository.save(adminUser);

        User endUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password(passwordEncoder.encode("password"))
                .name("End User")
                .roles(Set.of(Role.END_USER))
                .isActive(true)
                .build();
        userRepository.save(endUser);

        jwtToken = jwtUtil.generateToken(adminUser.getEmail(), Set.of("ROLE_ADMIN", "ROLE_SCRUM"));

    }

    private void initAuth() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_SCRUM")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    public void testGetAllUsers_Success() throws Exception {
        initAuth();
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testRegisterUser_Success() throws Exception {
        initAuth();
        UserRegistrationDTO registrationDTO = UserRegistrationDTO.builder()
                .email("newuser@example.com")
                .name("New User")
                .password("newPassword")
                .roles(Set.of("DEVELOPER")).build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDTO)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUserById_Unauthorized() throws Exception {
        initAdminContext();

        User user = userRepository.findAll().get(0);
        mockMvc.perform(get("/api/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateUser_Success() throws Exception {
        initAuth();
        User user = userRepository.findAll().get(1);
        UserDTO updateUser = UserDTO.builder()
                .name("Updated Name")
                .email(user.getEmail())
                .roles(Set.of("DEVELOPER"))
                .isActive(true).build();

        mockMvc.perform(put("/api/users/" + user.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUserById_SuccessWithScrumRole() throws Exception {
        User testUser = userRepository.findAll().get(0);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_SCRUM")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUserById_ForbiddenForNonScrumRole() throws Exception {
        User testUser = userRepository.findAll().get(0);

        initAdminContext();

        String nonScrumToken = jwtUtil.generateToken("anotheruser@example.com", Set.of("ROLE_DEVELOPER"));

        mockMvc.perform(get("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + nonScrumToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteUser_SuccessWithScrumRole() throws Exception {
        // Create a user to delete
        User testUser = userRepository.findAll().get(0);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@example.com", null, Set.of(new SimpleGrantedAuthority("ROLE_SCRUM")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // Make a request with a valid SCRUM role
        mockMvc.perform(delete("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + jwtToken) // jwtToken must have SCRUM role
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteUser_ForbiddenForNonScrumRole() throws Exception {
        // Create a user to delete
        User testUser = userRepository.findAll().get(0);

        initAdminContext();

        // Generate a JWT token without the SCRUM role
        String nonScrumToken = jwtUtil.generateToken("anotheruser@example.com", Set.of("ROLE_DEVELOPER"));

        // Make a request with a non-SCRUM role
        mockMvc.perform(delete("/api/users/" + testUser.getId())
                        .header("Authorization", "Bearer " + nonScrumToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    private static void initAdminContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@example.com", null);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}