package com.text2sql.text2sql_springboot.DTO;

import java.util.UUID;

public record UserTableDto(UUID id, String tablename, UUID userdatabaseid) {
}
