package com.techhub.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.entity.User;
import com.techhub.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    void testLoadByUsername_UserExists() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$hashedpasswordvaluehere1234567890");
        user.setRole("ADMIN");
        user.setStatus(1);
        user.setEmail("admin@test.com");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(user);

        // When
        UserDetails userDetails = service.loadUserByUsername("admin");

        // Then
        assertEquals("1", userDetails.getUsername());
        assertEquals("$2a$10$hashedpasswordvaluehere1234567890", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void testLoadByUsername_UserNotFound() {
        // Given
        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent"));
    }

    @Test
    void testLoadByUsername_UserBanned() {
        // Given
        User bannedUser = new User();
        bannedUser.setId(2L);
        bannedUser.setUsername("bannedUser");
        bannedUser.setPassword("$2a$10$hashedpasswordvaluehere1234567890");
        bannedUser.setRole("USER");
        bannedUser.setStatus(0);
        bannedUser.setEmail("banned@test.com");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(bannedUser);

        // When & Then
        assertThrows(DisabledException.class,
                () -> service.loadUserByUsername("bannedUser"));
    }
}
