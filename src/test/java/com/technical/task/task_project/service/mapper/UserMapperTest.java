package com.technical.task.task_project.service.mapper;


import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.service.dto.UserDTO;
import com.technical.task.task_project.service.dto.UserRegistrationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    public void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @ParameterizedTest
    @MethodSource("provideUsersForToDTO")
    public void testToDTO(User user) {
        UserDTO userDTO = userMapper.toDTO(user);

        assertNotNull(userDTO);
        assertEquals(user.getName(), userDTO.getName());
        assertEquals(user.getEmail(), userDTO.getEmail());
        assertEquals(user.isActive(), userDTO.isActive());
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                assertTrue(userDTO.getRoles().contains(role.name()));
            }
        }

    }

    private static Stream<Arguments> provideUsersForToDTO() {
        return Stream.of(
                Arguments.of(new User(UUID.randomUUID(), "Test User 1", "user1@example.com", null, Set.of(Role.DEVELOPER), true, null, null)),
                Arguments.of(new User(UUID.randomUUID(), "Test User 2", "user2@example.com", null, Set.of(Role.ADMIN, Role.SCRUM), false, null, null)),
                Arguments.of(new User(UUID.randomUUID(), "Test User 2", "user2@example.com", null, null, false, null, null))
        );
    }

    @ParameterizedTest
    @MethodSource("provideUserDTOsForToEntity")
    public void testToEntityFromUserDTO(UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);

        assertNotNull(user);
        assertEquals(userDTO.getName(), user.getName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertEquals(userDTO.isActive(), user.isActive());
        if (userDTO.getRoles() != null) {
            assertEquals(userDTO.getRoles(), user.getRoles().stream().map(String::valueOf).collect(Collectors.toSet()));
        }
        assertNull(user.getPassword());
    }

    private static Stream<Arguments> provideUserDTOsForToEntity() {
        return Stream.of(
                Arguments.of(UserDTO.builder().name("Test User 1").email("user1@example.com").roles(Set.of("DEVELOPER")).isActive(true).build()),
                Arguments.of(UserDTO.builder().name("Test User 2").email("user2@example.com").roles(Set.of("ADMIN", "SCRUM")).isActive(false).build())
                , Arguments.of(UserDTO.builder().name("Test User 2").email("user2@example.com").roles(null).isActive(false).build())
        );
    }

    @ParameterizedTest
    @MethodSource("provideUserRegistrationDTOsForToEntity")
    public void testToEntityFromUserRegistrationDTO(UserRegistrationDTO userRegistrationDTO) {
        User user = userMapper.toEntity(userRegistrationDTO);

        assertNotNull(user);
        assertNull(user.getId());
        assertEquals(userRegistrationDTO.getName(), user.getName());
        assertEquals(userRegistrationDTO.getEmail(), user.getEmail());
        assertTrue(user.isActive());
        if (userRegistrationDTO.getRoles() != null) {
            assertEquals(userRegistrationDTO.getRoles(), user.getRoles().stream().map(String::valueOf).collect(Collectors.toSet()));
        }
        assertEquals(userRegistrationDTO.getPassword(), user.getPassword());
    }

    private static Stream<Arguments> provideUserRegistrationDTOsForToEntity() {
        return Stream.of(
                Arguments.of(UserRegistrationDTO.builder().name("New User 1").email("newuser1@example.com").password("password1").roles(Set.of("DEVELOPER")).build()),
                Arguments.of(UserRegistrationDTO.builder().name("New User 2").email("newuser2@example.com").password("password2").roles(Set.of("ADMIN", "SCRUM")).build()),
                Arguments.of(UserRegistrationDTO.builder().name("New User 2").email("newuser2@example.com").password("password2").roles(null).build())
        );
    }

    @Test
    public void testToDTO_NullInput() {
        UserDTO result = userMapper.toDTO(null);
        assertNull(result, "Expected null when input User is null");
    }

    @Test
    public void testToEntityFromUserDTO_NullInput() {
        User result = userMapper.toEntity((UserDTO) null);
        assertNull(result, "Expected null when input UserDTO is null");
    }

    @Test
    public void testToEntityFromUserRegistrationDTO_NullInput() {
        User result = userMapper.toEntity((UserRegistrationDTO) null);
        assertNull(result, "Expected null when input UserRegistrationDTO is null");
    }


}
