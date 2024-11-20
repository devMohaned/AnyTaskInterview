package com.technical.task.task_project.service;

import com.technical.task.task_project.exception.ActionDisallowedException;
import com.technical.task.task_project.exception.AlreadyExistsException;
import com.technical.task.task_project.exception.NotFoundException;
import com.technical.task.task_project.model.User;
import com.technical.task.task_project.model.enums.Role;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.dto.UserDTO;
import com.technical.task.task_project.service.dto.UserRegistrationDTO;
import com.technical.task.task_project.service.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Log4j2
public class UserService {


    private final SecurityService securityService;
    private final UserRepository userRepository;

    private final UserMapper userMapper;


    public UserDTO registerUser(UserRegistrationDTO registrationDTO) {
        String email = registrationDTO.getEmail();
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            log.error("Email already exists. Failed to Register a new account. Email: [{}]", email);
            throw new AlreadyExistsException("Email is already in use.");
        }

        User user = userMapper.toEntity(registrationDTO);
        user.setPassword(securityService.encodePassword(registrationDTO.getPassword()));
        user = userRepository.save(user);
        return userMapper.toDTO(user);
    }

    public List<UserDTO> getAllUsers(boolean activeOnly) {
        List<User> users = activeOnly ? userRepository.findByIsActiveTrue() : userRepository.findAll();
        return users.stream().map(userMapper::toDTO).toList();
    }

    public UserDTO getUserById(UUID id) {
        User user = validateUserExistence(id);
        return userMapper.toDTO(user);
    }

    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmailAddress = authentication.getName();

        User user = validateUserExistence(id);
        if (user.getEmail().equals(currentEmailAddress)) {
            throw new ActionDisallowedException("You cannot update yourself");
        }

        if (!user.getEmail().equalsIgnoreCase(userDTO.getEmail())) {
            throw new ActionDisallowedException("You cannot change email addresses");
        }

        user.setName(userDTO.getName());
        user.setRoles(userDTO.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet()));
        user.setActive(userDTO.isActive());
        user = userRepository.save(user);
        return userMapper.toDTO(user);

    }


    public UserDTO softDeleteUser(UUID id) {
        User user = validateUserExistence(id);
        user.setActive(false);
        return userMapper.toDTO(userRepository.save(user));
    }

    public User validateUserExistence(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("Failed to get a user with UUID: [{}]", id);
            throw new NotFoundException("User does not exist");
        });
    }

    public User validateUserExistence(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("Failed to get a user with Email: [{}]", email);
            throw new NotFoundException("User does not exist");
        });
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmailAddress = authentication.getName();
        return validateUserExistence(currentEmailAddress);
    }
}