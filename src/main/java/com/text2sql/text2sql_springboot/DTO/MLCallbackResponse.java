package com.text2sql.text2sql_springboot.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MLCallbackResponse(
        @NotNull
        UUID jobId,
        @NotNull
        String queryResponse,
        @NotNull
        JobStatus status

) {
}
