package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.CreateUserDatabaseRequest;
import com.text2sql.text2sql_springboot.DTO.UserDatabaseDto;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDatabaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDatabaseRepository userDatabaseRepository;

    @InjectMocks
    private UserDatabaseService userDatabaseService;

    @Test
    void getAllUserDatabases_ShouldReturnDto_WhenPresent() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            String userId = "u-1";

            UserDatabase db1 = mock(UserDatabase.class);
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            when(db1.getId()).thenReturn(uuid1);
            when(db1.getDatabaseName()).thenReturn("DB One");

            UserDatabase db2 = mock(UserDatabase.class);
            when(db2.getId()).thenReturn(uuid2);
            when(db2.getDatabaseName()).thenReturn("DB Two");

            when(userDatabaseRepository.findByUserId(userId)).thenReturn(List.of(db1, db2));
            mocked.when(AppContext::getCurrentUserId).thenReturn(userId);
            List<UserDatabaseDto> result = userDatabaseService.getAllUserDatabases();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).filename()).isEqualTo("DB One");
            assertThat(result.get(0).id()).isEqualTo(uuid1);
            assertThat(result.get(1).filename()).isEqualTo("DB Two");
            assertThat(result.get(1).id()).isEqualTo(uuid2);

            verify(userDatabaseRepository).findByUserId(userId);
        }
    }

    @Test
    void getAllUserDatabases_shouldReturnEmptyList_whenRepoReturnsEmpty() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            String userId = "u-2";
            when(userDatabaseRepository.findByUserId(userId)).thenReturn(List.of());

            mocked.when(AppContext::getCurrentUserId).thenReturn(userId);
            List<UserDatabaseDto> result = userDatabaseService.getAllUserDatabases();

            assertThat(result).isEmpty();
            verify(userDatabaseRepository).findByUserId(userId);
        }
    }

    @Test
    void create_shouldPersistAndReturnDto() {
        CreateUserDatabaseRequest req = mock(CreateUserDatabaseRequest.class);
        when(req.user_id()).thenReturn("u-1");
        when(req.filename()).thenReturn("My DB");

        UserDetail user = new UserDetail("u-1", "u1@example.com");
        when(userRepository.getReferenceById("u-1")).thenReturn(user);

        UserDatabase persisted = new UserDatabase("My DB", user);
        when(userDatabaseRepository.save(any(UserDatabase.class))).thenReturn(persisted);

        UserDatabaseDto dto = userDatabaseService.create(req);

        assertEquals("My DB", dto.filename());
    }
}