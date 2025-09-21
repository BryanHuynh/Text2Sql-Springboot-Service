package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.TableVariableDto;
import com.text2sql.text2sql_springboot.DTO.TableVariableRequest;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.TableVariablesRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class TableVariableService {
    private final TableVariablesRepository tableVariablesRepository;
    private final UserTableRepository userTableRepository;
    private final UserDatabaseRepository userDatabaseRepository;

    public TableVariableService(TableVariablesRepository tableVariablesRepository, UserTableRepository userTableRepository, UserDatabaseRepository userDatabaseRepository) {
        this.tableVariablesRepository = tableVariablesRepository;
        this.userTableRepository = userTableRepository;
        this.userDatabaseRepository = userDatabaseRepository;
    }

    public List<TableVariableDto> getAllByTableId(UUID tableId) {
        UserTable table = userTableRepository.findById(tableId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        List<TableVariable> variables = tableVariablesRepository.findAllByUserTable(table);
        return variables.stream().map(TableVariableDto::new).toList();
    }

    public List<TableVariableDto> getAllVariablesWithinDatabase(UUID databaseID) {
        UserDatabase database = userDatabaseRepository.findById(databaseID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Database not found"));
        List<TableVariable> variables = tableVariablesRepository.findAll()
                .stream()
                .filter((tableVariable -> tableVariable
                        .getUserTable()
                        .getUserDatabase()
                        .getId().equals(database.getId())
                )).toList();
        return variables.stream().map(TableVariableDto::new).toList();
    }

    public TableVariableDto create(TableVariableRequest req) {
        UUID tableUUID = req.userTableId().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing user table id"));
        UserTable userTable = userTableRepository.findById(tableUUID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        TableVariable.Builder builder = new TableVariable.Builder()
                .variableName(req.variableName())
                .variableType(req.variableType())
                .pkFlag(req.pkFlag())
                .fkFlag(req.fkFlag())
                .userTable(userTable);
        if (req.referenceVariable().isPresent()) {
            TableVariable ref = tableVariablesRepository.findById(req.referenceVariable().get()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reference Variable not Found"));
            builder.fkRef(ref);
        }
        TableVariable saved = tableVariablesRepository.save(builder.build());
        return new TableVariableDto(saved);
    }

    public TableVariableDto update(TableVariableRequest req) {
        if (req.id().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID is required");
        TableVariable tableVariable = tableVariablesRepository.findById(req.id().get()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find variable"));
        tableVariable.setVariableName(req.variableName());
        tableVariable.setVariableType(req.variableType());
        tableVariable.setPkFlag(req.pkFlag());
        tableVariable.setFkFlag(req.fkFlag());
        if (req.referenceVariable().isPresent()) {
            TableVariable ref = tableVariablesRepository.findById(req.referenceVariable().get()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reference Variable not Found"));
            tableVariable.setFkRef(ref);
        }
        TableVariable saved = tableVariablesRepository.save(tableVariable);
        return new TableVariableDto(saved);
    }

    public void delete(UUID id) {
        tableVariablesRepository.deleteById(id);
    }
}
