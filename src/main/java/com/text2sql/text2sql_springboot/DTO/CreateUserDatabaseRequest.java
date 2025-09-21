package com.text2sql.text2sql_springboot.DTO;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;
import java.util.UUID;

public record CreateUserDatabaseRequest(
        @NotBlank
        String filename,

        @Id
        @NotBlank
        String user_id,

        @Id
        Optional<UUID> database_id
) {
}
