package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.SchemaModel;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.TableVariablesRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SchemaModelConstructionService {
    private final TableVariablesRepository tableVariablesRepository;
    private final UserDatabaseRepository userDatabaseRepository;
    private final UserTableRepository userTableRepository;

    public SchemaModelConstructionService(
            TableVariablesRepository tableVariablesRepository,
            UserDatabaseRepository userDatabaseRepository,
            UserTableRepository userTableRepository
    ) {
        this.tableVariablesRepository = tableVariablesRepository;
        this.userDatabaseRepository = userDatabaseRepository;
        this.userTableRepository = userTableRepository;
    }

    public SchemaModel constructSchema(UUID dbId) throws ResponseStatusException {
        Optional<UserDatabase> db = userDatabaseRepository.findById(dbId);
        if (db.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database not found");
        }
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.setDbId(db.get().getDatabaseName());

        List<UserTable> tables = userTableRepository.findByUserDatabase(db.get());
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
