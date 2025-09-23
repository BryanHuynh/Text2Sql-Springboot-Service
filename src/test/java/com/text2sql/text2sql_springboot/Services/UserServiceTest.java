package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.CreateUpdateUserRequest;
import com.text2sql.text2sql_springboot.DTO.UserDto;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;


    @Test
    void ShouldReturnUserDto_findById_WhenValidAppContext() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            String user_id = "123";
            String user_email = "testEmail@email.com";
            UserDetail userDetail = new UserDetail(user_id, user_email);
            mocked.when(AppContext::getCurrentUserId).thenReturn("123");
            when(userRepository.findById(user_id)).thenReturn(Optional.of(userDetail));
            assertEquals(new UserDto(user_id, user_email), userService.findById());
        }
    }

    @Test
    void ShouldReturnNotFound_findById_WhenNotFound() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            mocked.when(AppContext::getCurrentUserId).thenReturn("123");
            when(userRepository.findById("123")).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class, () -> userService.findById());
        }
    }

    @Test
    void create_shouldThrowConflict_whenIdExists() {
        var req = new CreateUpdateUserRequest("u-1", "taken@example.com");
        when(userRepository.existsById("u-1")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> userService.create(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowConflict_whenEmailExists() {
        var req = new CreateUpdateUserRequest("u-1", "taken@example.com");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> userService.create(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldReturnDto_whenNotExists() {

        var req = new CreateUpdateUserRequest("u-2", "new@example.com");

        when(userRepository.existsById("u-2")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        var persisted = new UserDetail("u-2", "new@example.com");
        when(userRepository.save(any(UserDetail.class))).thenReturn(persisted);

        UserDto result = userService.create(req);

        assertEquals("u-2", result.id());
        assertEquals("new@example.com", result.email());
    }

    @Test
    void delete_shouldThrowNotFound_whenUserDoesNotExist() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            mocked.when(AppContext::getCurrentUserId).thenReturn("u-1");
            when(userRepository.existsById("u-1")).thenReturn(false);
            var ex = assertThrows(ResponseStatusException.class, () -> userService.delete());
            assertThat(ex.getReason()).contains("u-1 not found");
            verify(userRepository, never()).deleteById(any());
        }
    }

    @Test
    void delete_shouldRemoveUser_whenExists() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            mocked.when(AppContext::getCurrentUserId).thenReturn("u-2");
            when(userRepository.existsById("u-2")).thenReturn(true);
            userService.delete();
            verify(userRepository).deleteById("u-2");
        }
    }
}