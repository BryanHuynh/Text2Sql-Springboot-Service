package com.text2sql.text2sql_springboot.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;


public record MLPingDto(
        @NotNull
        Boolean ok,

        @JsonProperty("ack_time")
        @NotNull
        LocalDateTime ackTime
) {
}
