package com.text2sql.text2sql_springboot.DTO;

import java.util.Optional;
import java.util.UUID;

public record UserTableRequest(
        String tablename,
        UUID userDatabaseId,
        Optional<UUID> id
) {
}
