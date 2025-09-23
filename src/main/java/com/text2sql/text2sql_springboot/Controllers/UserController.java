package com.text2sql.text2sql_springboot.Controllers;

import com.text2sql.text2sql_springboot.DTO.CreateUpdateUserRequest;
import com.text2sql.text2sql_springboot.DTO.UserDto;
import com.text2sql.text2sql_springboot.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService userService) {
        this.service = userService;
    }

    @GetMapping
    public UserDto getUserById() {
        return service.findById();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUpdateUserRequest req) {
        return service.create(req);
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() {
        service.delete();
    }
}
