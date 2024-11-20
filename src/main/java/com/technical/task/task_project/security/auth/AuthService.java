package com.technical.task.task_project.security.auth;

import com.technical.task.task_project.model.User;
import com.technical.task.task_project.repository.UserRepository;
import com.technical.task.task_project.service.mapper.RoleToAuthorityMapper;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleToAuthorityMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert User entity to Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                roleMapper.mapRolesToAuthorities(user.getRoles())
        );
    }
}