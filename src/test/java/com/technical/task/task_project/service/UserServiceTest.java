package com.technical.task.task_project.service;

import com.technical.task.task_project.exception.ActionDisallowedException;
import com.technical.task.task_project.exception.AlreadyExistsException;
import com.technical.task.task_project.exception.NotFoundException;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.dto.UserDTO;
import com.technical.task.task_project.service.dto.UserRegistrationDTO;
import com.technical.task.task_project.service.mapper.UserMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapperImpl userMapper;

    String firstEmail = "test@example.com";

    @BeforeEach
    public void setUp() {

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null, List.of());
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        userRepository.deleteAll();

        User sampleUser = User.builder().id(UUID.randomUUID()).email(firstEmail)
                .name("Test User")
                .password("Passwrod")
                .roles(Set.of(Role.DEVELOPER))
                .isActive(true).build();

        User sampleUser2 = User.builder().id(UUID.randomUUID()).email("AnotherEmail@gmail.com")
                .name("Test User")
                .password("Passwrod")
                .roles(Set.of(Role.DEVELOPER))
                .isActive(true).build();

        userRepository.save(sampleUser);
        userRepository.save(sampleUser2);

    }

    private UserRegistrationDTO buildSampleRegistration() {
        return UserRegistrationDTO.builder().email("newuser@example.com")
                .name("New User")
                .roles(Set.of("ADMIN")).password("SomePassword").build();
    }

    @Test
    public void testRegisterUser_Success() {
        UserDTO result = userService.registerUser(buildSampleRegistration());

        assertNotNull(result);
        assertEquals("newuser@example.com", result.getEmail());
        Optional<User> savedUser = userRepository.findByEmail("newuser@example.com");
        assertTrue(savedUser.isPresent());
        assertEquals("New User", savedUser.get().getName());
    }

    @Test
    public void testRegisterUser_AlreadyExists() {
        UserRegistrationDTO sampleRegistrationDTO = buildSampleRegistration();
        sampleRegistrationDTO.setEmail("test@example.com");

        assertThrows(AlreadyExistsException.class, () -> userService.registerUser(sampleRegistrationDTO));
    }

    @Test
    public void testGetAllUsers_ActiveOnly() {
        List<UserDTO> result = userService.getAllUsers(true);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        assertEquals("AnotherEmail@gmail.com", result.get(1).getEmail());
    }

    @Test
    public void testGetAllUsers_AllUsers() {
        List<User> all = userRepository.findAll();
        User sampleUser = all.get(0);
        User sampleUser2 = all.get(1);
        sampleUser.setActive(false);
        userRepository.save(sampleUser);

        List<UserDTO> result = userService.getAllUsers(false);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(sampleUser.getEmail(), result.get(0).getEmail());
        assertEquals(sampleUser2.getEmail(), result.get(1).getEmail());
    }

    @Test
    public void testGetUserById_Success() {
        User sampleUser = userRepository.findByEmail(firstEmail).get();
        UserDTO result = userService.getUserById(sampleUser.getId());

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    public void testGetUserById_NotFound() {
        UUID randomId = UUID.randomUUID();

        assertThrows(NotFoundException.class, () -> userService.getUserById(randomId));
    }

    @Test
    public void testUpdateUser_Success() {
        User exisitingUser = userRepository.findAll().get(1);
        exisitingUser.setName("Updated Name");
        UserDTO updateDTO = userMapper.toDTO(exisitingUser);
        UserDTO result = userService.updateUser(exisitingUser.getId(), updateDTO);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }


    @Test
    public void testUpdateUser_FailedCannotUpdateYourself() {
        UserDTO updateDTO = UserDTO.builder().name("Updated Name")
                .email("updated@example.com").build();


        ActionDisallowedException result =
                assertThrows(ActionDisallowedException.class, () -> userService.updateUser(userRepository.findByEmail(firstEmail).get().getId(), updateDTO));

        assertNotNull(result);
        assertEquals("You cannot update yourself", result.getMessage());
    }

    @Test
    public void testUpdateUser_NotFound() {
        UUID randomId = UUID.randomUUID();
        UserDTO updateDTO = UserDTO.builder().build();

        assertThrows(NotFoundException.class, () -> userService.updateUser(randomId, updateDTO));
    }

    @Test
    public void testUpdateUser_DisallowedAction() {
        User sampleUser = userRepository.findByEmail(firstEmail).get();
        sampleUser.setEmail("disallowed@example.com");
        userRepository.save(sampleUser);
        assertThrows(ActionDisallowedException.class, () -> userService.updateUser(sampleUser.getId(), UserDTO.builder().build()));
    }

    @Test
    public void testSoftDeleteUser() {
        User sampleUser = userRepository.findByEmail(firstEmail).get();
        userService.softDeleteUser(sampleUser.getId());

        Optional<User> deletedUser = userRepository.findById(sampleUser.getId());
        assertTrue(deletedUser.isPresent());
        assertFalse(deletedUser.get().isActive());
    }

    @Test
    public void testSoftDeleteUser_NotFound() {
        UUID randomId = UUID.randomUUID();

        assertThrows(NotFoundException.class, () -> userService.softDeleteUser(randomId));
    }

    @Test
    public void testCurrentUser_NotFound() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("nonExistence@example.com", null, List.of());
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        assertThrows(NotFoundException.class, () -> userService.getCurrentUser());
    }

    @Test
    public void testCurrentUser_Found() {
        assertNotNull(userService.getCurrentUser());
    }
}
