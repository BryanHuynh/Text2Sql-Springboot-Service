package com.text2sql.text2sql_springboot.DTO;

import java.util.Optional;
import java.util.UUID;

public record TableVariableRequest(
        Optional<UUID> id,
        String variableName,
        String variableType,
        boolean pk_flag,
        boolean fk_flag,
        Optional<UUID> userTableId,
        Optional<UUID> referenceVariable,
        Optional<Integer> order
) {
}
