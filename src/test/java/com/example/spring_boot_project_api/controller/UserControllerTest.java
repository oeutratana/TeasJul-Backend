package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.UserHouseAccessService;
import com.example.spring_boot_project_api.service.UserRoleService;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.JwtUtil;

import org.springframework.data.domain.Pageable;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserHouseAccessService userHouseAccessService;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "johndoe", "john@example.com", "John", "Doe",
                "+1234567890", null, UserStatus.ACTIVE, null, null, null);
    }

    private UserDetails springUser() {
        return User.builder()
                .username("johndoe")
                .password("n/a")
                .roles("USER")
                .build();
    }

    private String validToken() {
        return jwtUtil.generateToken("johndoe");
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointShouldRejectInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointShouldAcceptValidJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.getAllUsers(any(), any(), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(sampleUser()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("johndoe"));
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedUserWithValidJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.getCurrentUser()).thenReturn(sampleUser());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentUserShouldFailWhenTokenSubjectUnknown() throws Exception {
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found: ghost"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("ghost")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserRolesShouldReturnRolesWithValidJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        var role = new com.example.spring_boot_project_api.dto.response.RoleResponse(
                1L, "Owner", "OWNER", "Owns houses", true, null, null);
        when(userRoleService.getCurrentUserRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/v1/users/me/roles")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("OWNER"));
    }

    @Test
    void getUserByIdShouldReturnUserWithValidJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.getUserById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/api/v1/users/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUserByIdShouldReturnNotFoundForMissingUser() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.getUserById(99L)).thenThrow(UserNotFoundException.forId(99L));

        mockMvc.perform(get("/api/v1/users/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void updateUserShouldUpdateAndReturnUser() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.updateUser(anyLong(), any())).thenReturn(sampleUser());

        mockMvc.perform(put("/api/v1/users/1")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Johnny",
                                  "phone": "+9876543210"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void deleteUserShouldReturnNoContent() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());

        mockMvc.perform(delete("/api/v1/users/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void availableForTenantRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/available-for-tenant"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availableForTenantWithRoleAllowsAccess() throws Exception {
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(springUser());
        when(userService.searchAvailableForTenant(any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0L, 0));
        mockMvc.perform(get("/api/v1/users/available-for-tenant")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }
}
