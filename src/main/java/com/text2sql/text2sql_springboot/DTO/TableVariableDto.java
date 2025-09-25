package com.text2sql.text2sql_springboot.DTO;

import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import io.micrometer.common.lang.Nullable;

import java.util.Optional;
import java.util.UUID;

public record TableVariableDto(
        UUID id,
        String variableName,
        String variableType,
        Boolean pk_flag,
        Boolean fk_flag,
        UserTableDto userTable,
        @Nullable TableVariableDto referenceTable,
        int order
) {
    public TableVariableDto(TableVariable variable) {
        this(
                variable.getId(),
                variable.getVariableName(),
                variable.getVariableType(),
                variable.isPkFlag(),
                variable.isFkFlag(),
                variable.getUserTable() != null ? new UserTableDto(variable.getUserTable()) : null,
                variable.getFkRef() != null ? new TableVariableDto(variable.getFkRef()) : null,
                variable.getOrder()
        );
    }
}