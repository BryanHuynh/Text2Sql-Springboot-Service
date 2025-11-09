package com.text2sql.text2sql_springboot.DTO;

import com.text2sql.text2sql_springboot.Entities.TableVariable;

import java.util.Optional;
import java.util.UUID;

public record TableVariableRequest(
        Optional<UUID> id,
        String variableName,
        String variableType,
        boolean pk_flag,
        boolean fk_flag,
        Optional<UUID> userTableId,
        Optional<TableVariable> referenceVariable,
        Optional<Integer> order
) {
}
