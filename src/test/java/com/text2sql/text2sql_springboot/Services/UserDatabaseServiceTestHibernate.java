package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.CreateUpdateUserRequest;
import com.text2sql.text2sql_springboot.DTO.CreateUserDatabaseRequest;
import com.text2sql.text2sql_springboot.DTO.UserDatabaseDto;
import com.text2sql.text2sql_springboot.DTO.UserDto;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DataJpaTest
class UserDatabaseServiceTestHibernate {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDatabaseRepository userDatabaseRepository;

    private UserService userService;

    private UserDatabaseService userDatabaseService;

    private String userId = "123";

    @BeforeEach
    void beforeEach() {
        userService = new UserService(userRepository);
        CreateUpdateUserRequest req = new CreateUpdateUserRequest(userId, "test@email.me");
        UserDto saved = userService.create(req);
    }

    @Test
    void createUserDatabase() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            mocked.when(AppContext::getCurrentUserId).thenReturn(userId);
            userDatabaseService = new UserDatabaseService(userDatabaseRepository, userRepository);
            UserDatabaseDto saved = userDatabaseService.create(new CreateUserDatabaseRequest("db_1", null));
            assertThat(saved.filename()).isEqualTo("db_1");
        }
    }

    @Test
    void updateUserDatabase() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            mocked.when(AppContext::getCurrentUserId).thenReturn(userId);
            userDatabaseService = new UserDatabaseService(userDatabaseRepository, userRepository);
            UserDatabaseDto saved = userDatabaseService.create(new CreateUserDatabaseRequest("db_1", null));

            UserDatabaseDto updated = userDatabaseService.update(new CreateUserDatabaseRequest("db_2", Optional.ofNullable(saved.id())));
            assertThat(updated.filename()).isEqualTo("db_2");
        }
    }


}