package com.text2sql.text2sql_springboot.Controllers;

import com.text2sql.text2sql_springboot.DTO.UserTableDto;
import com.text2sql.text2sql_springboot.DTO.UserTableRequest;
import com.text2sql.text2sql_springboot.Services.UserTableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/userTables")
public class UserTableController {
    private final UserTableService userTableService;

    public UserTableController(UserTableService userTableService) {
        this.userTableService = userTableService;
    }

    @GetMapping("/{fileid}")
    public ResponseEntity<List<UserTableDto>> getAllUserTablesByFileId(@PathVariable UUID fileid) {
        List<UserTableDto> tables = userTableService.findAllByDatabaseId(fileid);
        return new ResponseEntity<>(tables, HttpStatus.OK);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserTableDto> createUserTable(@Valid @RequestBody UserTableRequest userTableRequest) {
        var created = userTableService.create(userTableRequest);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    public void deleteUserTable(@PathVariable UUID id, @RequestHeader String user_id) {
        userTableService.delete(id, user_id);
    }

    @PostMapping("/update")
    public ResponseEntity<UserTableDto> updateUserTable(@RequestBody UserTableRequest req, @RequestHeader String user_id) {
        return ResponseEntity.ok(userTableService.update(req, user_id));
    }
}
