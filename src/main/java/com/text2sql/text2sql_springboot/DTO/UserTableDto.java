package com.text2sql.text2sql_springboot.DTO;

import com.text2sql.text2sql_springboot.Entities.UserTable;

import java.util.UUID;

public record UserTableDto(UUID id, String tablename, UUID userdatabaseid) {
    public UserTableDto(UserTable userTable) {
        this(
                userTable.getId(),
                userTable.getTableName(),
                userTable.getUserDatabase().getId()
        );
    }
}
