package com.text2sql.text2sql_springboot.Controllers;

import com.text2sql.text2sql_springboot.DTO.CreateUserDatabaseRequest;
import com.text2sql.text2sql_springboot.DTO.UserDatabaseDto;
import com.text2sql.text2sql_springboot.Services.UserDatabaseService;
import jakarta.validation.Valid;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/userDatabases")
public class UserDatabaseController {
    private final UserDatabaseService userDatabaseService;

    public UserDatabaseController(UserDatabaseService userFileService) {
        this.userDatabaseService = userFileService;
    }

    @GetMapping("/{userId}")
    public List<UserDatabaseDto> getAllUserDatabases(@PathVariable String userId) {
        return userDatabaseService.getAllUserDatabases(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserDatabaseDto> createUserFile(@Valid @RequestBody CreateUserDatabaseRequest userFile) {
        var created = userDatabaseService.create(userFile);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/update")
    public ResponseEntity<UserDatabaseDto> updateUserDatabase(@Valid @RequestBody CreateUserDatabaseRequest req) {
        var updated = userDatabaseService.update(req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserFile(@PathVariable UUID id, @RequestHeader String user_id) {
        userDatabaseService.delete(id, user_id);
    }
}