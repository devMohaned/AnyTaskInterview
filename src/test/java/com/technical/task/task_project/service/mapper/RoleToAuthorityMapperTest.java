package com.technical.task.task_project.service.mapper;

import com.technical.task.task_project.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RoleToAuthorityMapperTest {

    private RoleToAuthorityMapper roleToAuthorityMapper;

    @BeforeEach
    public void setUp() {
        roleToAuthorityMapper = new RoleToAuthorityMapper() {
        };
    }

    @Test
    public void testMapRolesToAuthorities_WithRoles() {
        Set<Role> roles = Set.of(Role.ADMIN, Role.DEVELOPER);

        Set<SimpleGrantedAuthority> result = roleToAuthorityMapper.mapRolesToAuthorities(roles);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(result.contains(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    }

    @Test
    public void testMapRolesToAuthorities_EmptyRoles() {
        Set<Role> roles = Collections.emptySet();

        Set<SimpleGrantedAuthority> result = roleToAuthorityMapper.mapRolesToAuthorities(roles);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
