package com.technical.task.task_project.service;

import com.technical.task.task_project.security.jwt.JwtUtil;
import com.technical.task.task_project.service.dto.AuthenticationRequestDTO;
import com.technical.task.task_project.service.mapper.RoleToAuthorityMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final UserDetailsService userDetailsService;

    private final RoleToAuthorityMapper roleMapper;

    public String authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        Set<String> assignedRoles = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        return jwtUtil.generateToken(userDetails.getUsername(), assignedRoles);
    }
}
