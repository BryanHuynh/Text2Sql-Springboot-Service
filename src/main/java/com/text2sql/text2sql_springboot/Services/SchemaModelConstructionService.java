package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.TableVariablesRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaModelConstructionService {
    private final TableVariablesRepository tableVariablesRepository;
    private final UserTableRepository userTableRepository;

    public SchemaModelConstructionService(
            TableVariablesRepository tableVariablesRepository,
            UserTableRepository userTableRepository
    ) {
        this.tableVariablesRepository = tableVariablesRepository;
        this.userTableRepository = userTableRepository;
    }

    public SchemaModel constructSchema(UserDatabase db) throws ResponseStatusException {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.setDbId(db.getDatabaseName());

        List<UserTable> tables = userTableRepository.findByUserDatabase(db);
        if (tables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Tables Present");
        }
        for (UserTable table : tables) {
            builder.addTable(table.getTableName());
        }

        List<TableVariable> tableVariables = new ArrayList<>();
        for (UserTable table : tables) {
            List<TableVariable> variables = tableVariablesRepository.findAllByUserTable(table);
            tableVariables.addAll(variables);
        }
        if (tableVariables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Table Variables Present");
        }

        tableVariables.forEach(tableVariable -> {
            String tableName = tableVariable.getUserTable().getTableName();
            String variableName = tableVariable.getVariableName();
            String type = tableVariable.getVariableType();
            builder.addColumn(tableName, variableName, type);
            if (tableVariable.isPkFlag()) {
                builder.addPrimaryKey(tableName, variableName);
            }
        });

        tableVariables.forEach(tableVariable -> {
            if (tableVariable.isFkFlag()) {
                TableVariable primary = tableVariable.getFkRef();
                String primaryTable = primary.getUserTable().getTableName();
                String primaryVariable = primary.getVariableName();
                String fkTable = tableVariable.getUserTable().getTableName();

                builder.addForeignKey(primaryTable, primaryVariable, fkTable, tableVariable.getVariableName());
            }
        });
        return builder.build();
    }
}
