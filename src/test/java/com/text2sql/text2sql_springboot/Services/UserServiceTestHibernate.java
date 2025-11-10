package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.CreateUpdateUserRequest;
import com.text2sql.text2sql_springboot.DTO.UserDto;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DataJpaTest
class UserServiceTestHibernate {

    @Autowired
    private UserRepository repo;

    @Autowired
    private UserService userService;

    @Test
    void create_and_findById() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            userService = new UserService(repo);
            CreateUpdateUserRequest req = new CreateUpdateUserRequest("123", "test@email.me");
            UserDto saved = userService.create(req);
            assertThat(saved.id()).isEqualTo(req.id());

            mocked.when(AppContext::getCurrentUserId).thenReturn("123");
            UserDto found = userService.findById();
            assertThat(found.id()).isEqualTo(req.id());
        }
    }

    @Test
    void create_and_delete() {
        try (MockedStatic<AppContext> mocked = mockStatic(AppContext.class)) {
            userService = new UserService(repo);
            CreateUpdateUserRequest req = new CreateUpdateUserRequest("123", "test@email.me");
            UserDto saved = userService.create(req);
            assertThat(saved.id()).isEqualTo(req.id());

            mocked.when(AppContext::getCurrentUserId).thenReturn("123");
            userService.delete();

            assertThrows(ResponseStatusException.class, () -> userService.findById());
        }
    }

}