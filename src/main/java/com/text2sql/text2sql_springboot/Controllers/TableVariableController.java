package com.text2sql.text2sql_springboot.Controllers;

import com.text2sql.text2sql_springboot.DTO.TableVariableDto;
import com.text2sql.text2sql_springboot.DTO.TableVariableRequest;
import com.text2sql.text2sql_springboot.Services.TableVariableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tableVariables")
public class TableVariableController {
    private final TableVariableService tableVariableService;

    public TableVariableController(TableVariableService tableVariableService) {
        this.tableVariableService = tableVariableService;
    }

    @GetMapping("/{tableId}")
    public List<TableVariableDto> getAllVariablesForTable(@PathVariable UUID tableId) {
        return tableVariableService.getAllByTableId(tableId);
    }

    @GetMapping("all/{database_id}")
    public List<TableVariableDto> getAllVariables(@PathVariable UUID database_id) {
        return tableVariableService.getAllVariablesWithinDatabase(database_id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TableVariableDto> createTableVariable(@Valid @RequestBody TableVariableRequest req) {
        var created = tableVariableService.create(req);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/update")
    public ResponseEntity<TableVariableDto> updateTableVariable(@Valid @RequestBody TableVariableRequest req) {
        var updated = tableVariableService.update(req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{variableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTableVariable(@PathVariable UUID variableId) {
        tableVariableService.delete(variableId);
    }
}
