package com.text2sql.text2sql_springboot.DTO;

import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUpdateUserRequest(
        @Id @NotBlank String id,
        @NotBlank @Email String email) {
}
