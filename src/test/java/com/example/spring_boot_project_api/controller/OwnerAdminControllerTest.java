package com.example.spring_boot_project_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.OwnerAdminService;
import com.example.spring_boot_project_api.util.JwtUtil;

import org.springframework.data.domain.Pageable;

@WebMvcTest(OwnerAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class OwnerAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private OwnerAdminService ownerAdminService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleOwner() {
        return new UserResponse(1L, "newowner", "newowner@rental.com", "Nika", "Chea",
                "+85512345678", null, UserStatus.ACTIVE, null, null, null);
    }

    private UserDetails springUser() {
        return User.builder()
                .username("boss")
                .password("n/a")
                .roles("ADMIN")
                .build();
    }

    private String validToken() {
        return jwtUtil.generateToken("boss");
    }

    private void allowJwt() throws Exception {
        when(userDetailsService.loadUserByUsername("boss")).thenReturn(springUser());
    }

    @Test
    void createOwnerShouldReturnCreatedWithValidJwt() throws Exception {
        allowJwt();
        when(ownerAdminService.createOwner(any())).thenReturn(sampleOwner());

        mockMvc.perform(post("/api/admin/owners")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newowner",
                                  "email": "newowner@rental.com",
                                  "password": "Password123",
                                  "firstName": "Nika",
                                  "lastName": "Chea",
                                  "phone": "+85512345678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newowner"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createOwnerShouldIgnoreForgedRoleField() throws Exception {
        allowJwt();
        when(ownerAdminService.createOwner(any())).thenReturn(sampleOwner());

        mockMvc.perform(post("/api/admin/owners")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newowner",
                                  "email": "newowner@rental.com",
                                  "password": "Password123",
                                  "firstName": "Nika",
                                  "lastName": "Chea",
                                  "role": "SUPER_ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());

        org.mockito.ArgumentCaptor<com.example.spring_boot_project_api.dto.request.CreateOwnerRequest> captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.example.spring_boot_project_api.dto.request.CreateOwnerRequest.class);
        verify(ownerAdminService).createOwner(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("newowner");
        assertThat(captor.getValue().email()).isEqualTo("newowner@rental.com");
    }

    @Test
    void createOwnerShouldRejectMissingRequiredFields() throws Exception {
        allowJwt();

        mockMvc.perform(post("/api/admin/owners")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "nn",
                                  "email": "not-an-email",
                                  "firstName": "Nika"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOwnersShouldReturnPagedOwnersWithValidJwt() throws Exception {
        allowJwt();
        when(ownerAdminService.listOwners(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(PageResponse.of(List.of(sampleOwner()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/admin/owners")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("newowner"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void getOwnerShouldReturnOwnerWithValidJwt() throws Exception {
        allowJwt();
        when(ownerAdminService.getOwner(1L)).thenReturn(sampleOwner());

        mockMvc.perform(get("/api/admin/owners/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getOwnerShouldReturnStatus404ForMissingOwner() throws Exception {
        allowJwt();
        when(ownerAdminService.getOwner(99L)).thenThrow(UserNotFoundException.forId(99L));

        mockMvc.perform(get("/api/admin/owners/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void updateOwnerShouldReturnUpdatedOwnerWithValidJwt() throws Exception {
        allowJwt();
        when(ownerAdminService.updateOwner(anyLong(), any())).thenReturn(sampleOwner());

        mockMvc.perform(put("/api/admin/owners/1")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Nika",
                                  "phone": "+999"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nika"));
    }

    @Test
    void updateOwnerStatusShouldReturnUpdatedOwnerWithValidJwt() throws Exception {
        allowJwt();
        UserResponse inactive = new UserResponse(1L, "newowner", "newowner@rental.com",
                "Nika", "Chea", "+85512345678", null, UserStatus.INACTIVE, null, null, null);
        when(ownerAdminService.updateOwnerStatus(1L, UserStatus.INACTIVE)).thenReturn(inactive);

        mockMvc.perform(patch("/api/admin/owners/1/status")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateOwnerStatusShouldRejectMissingStatus() throws Exception {
        allowJwt();

        mockMvc.perform(patch("/api/admin/owners/1/status")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/admin/owners"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpointShouldRejectInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/admin/owners")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}