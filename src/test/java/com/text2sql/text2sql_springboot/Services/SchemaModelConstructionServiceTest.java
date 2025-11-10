package com.text2sql.text2sql_springboot.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.TableVariablesRepository;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaModelConstructionServiceTest {
    @Mock
    private TableVariablesRepository tableVariablesRepository;
    @Mock
    private UserDatabaseRepository userDatabaseRepository;
    @Mock
    private UserTableRepository userTableRepository;

    @InjectMocks
    private SchemaModelConstructionService schemaModelConstructionService;

    @Test
    void ShouldReturnValidSchema_ConstructSchema_WhenDataPresent() throws JsonProcessingException {
        UserDetail userDetail = new UserDetail("001", "test@email.com");
        UserDatabase userDatabase = new UserDatabase("warehouse_1", userDetail);

        UserTable warehouseTable = new UserTable("Warehouses", userDatabase);
        UserTable boxesTable = new UserTable("Boxes", userDatabase);

        TableVariable warehouseCode = new TableVariable(
                new TableVariable.Builder()
                        .userTable(warehouseTable)
                        .variableName("Code")
                        .variableType("number")
                        .pkFlag(true));
        TableVariable warehouseLocation = new TableVariable(
                new TableVariable.Builder()
                        .userTable(warehouseTable)
                        .variableName("Location")
                        .variableType("text")
        );
        TableVariable warehouseCapacity = new TableVariable(
                new TableVariable.Builder()
                        .userTable(warehouseTable)
                        .variableName("Capacity")
                        .variableType("number")
        );
        TableVariable boxesCode = new TableVariable(
                new TableVariable.Builder()
                        .userTable(boxesTable)
                        .variableName("Code")
                        .variableType("text")
                        .pkFlag(true)
        );
        TableVariable boxesContents = new TableVariable(
                new TableVariable.Builder()
                        .userTable(boxesTable)
                        .variableName("Contents")
                        .variableType("text")
        );
        TableVariable boxesValue = new TableVariable(
                new TableVariable.Builder()
                        .userTable(boxesTable)
                        .variableName("Value")
                        .variableType("number")
        );
        TableVariable boxesWarehouse = new TableVariable(
                new TableVariable.Builder()
                        .userTable(boxesTable)
                        .variableName("Warehouse")
                        .variableType("number")
                        .fkFlag(true)
                        .fkRef(warehouseCode)
        );

        when(userDatabaseRepository.findById(userDatabase.getId())).thenReturn(Optional.of(userDatabase));
        when(userTableRepository.findByUserDatabase(userDatabase)).thenReturn(List.of(warehouseTable, boxesTable));
        when(tableVariablesRepository.findAllByUserTable(warehouseTable)).thenReturn(List.of(warehouseCode, warehouseLocation, warehouseCapacity));
        when(tableVariablesRepository.findAllByUserTable(boxesTable)).thenReturn(List.of(boxesCode, boxesContents, boxesValue, boxesWarehouse));


        Map<String, Object> expectedSchema = new HashMap<>();
        List<List<Object>> columnNames = new ArrayList<>();
        columnNames.add(Arrays.asList(-1, "*"));
        columnNames.add(Arrays.asList(0, "Code"));
        columnNames.add(Arrays.asList(0, "Location"));
        columnNames.add(Arrays.asList(0, "Capacity"));
        columnNames.add(Arrays.asList(1, "Code"));
        columnNames.add(Arrays.asList(1, "Contents"));
        columnNames.add(Arrays.asList(1, "Value"));
        columnNames.add(Arrays.asList(1, "Warehouse"));
        expectedSchema.put("column_names_original", columnNames);

        List<String> columnTypes = List.of("text", "number", "text", "number", "text", "text", "number", "number");
        expectedSchema.put("column_types", columnTypes);

        List<String> tableNames = List.of("Warehouses", "Boxes");
        expectedSchema.put("table_names_original", tableNames);

        List<List<Integer>> foreignKeys = List.of(List.of(7, 1));
        expectedSchema.put("foreign_keys", foreignKeys);

        List<Integer> primaryKeys = List.of(1, 4);
        expectedSchema.put("primary_keys", primaryKeys);

        expectedSchema.put("db_id", "warehouse_1");

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedSchema);

        assertEquals(expectedJson, schemaModelConstructionService.constructSchema(userDatabase.getId()).toSchemaJson());
    }

    @Test
    void ShouldReturnBadRequest_constructSchema_WhenDbIdIsInvalid() {
        UUID uuid = UUID.randomUUID();
        when(userDatabaseRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            schemaModelConstructionService.constructSchema(uuid);
        });
    }


    @Test
    void ShouldReturnBadRequest_constructSchema_WhenNoTablesExist() {
        UserDetail userDetail = new UserDetail("001", "test@email.com");
        UserDatabase userDatabase = new UserDatabase("warehouse_1", userDetail);
        when(userDatabaseRepository.findById(userDatabase.getId())).thenReturn(Optional.of(userDatabase));
        when(userTableRepository.findByUserDatabase(userDatabase)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> {
            schemaModelConstructionService.constructSchema(userDatabase.getId());
        });
    }

    @Test
    void ShouldReturnBadRequest_constructSchema_WhenNoVariablesExist() {
        UserDetail userDetail = new UserDetail("001", "test@email.com");
        UserDatabase userDatabase = new UserDatabase("warehouse_1", userDetail);
        UserTable warehouseTable = new UserTable("Warehouses", userDatabase);

        when(userDatabaseRepository.findById(userDatabase.getId())).thenReturn(Optional.of(userDatabase));
        when(userTableRepository.findByUserDatabase(userDatabase)).thenReturn(List.of(warehouseTable));
        when(tableVariablesRepository.findAllByUserTable(warehouseTable)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> {
            schemaModelConstructionService.constructSchema(userDatabase.getId());
        });
    }

    @Test
    void shouldReturnValidSchema_constructSchema_WhenOnlyOneVariableExistsAmongstMultipleTables() throws JsonProcessingException {
        UserDetail userDetail = new UserDetail("001", "test@email.com");
        UserDatabase userDatabase = new UserDatabase("warehouse_1", userDetail);
        UserTable warehouseTable = new UserTable("Warehouses", userDatabase);
        UserTable boxesTable = new UserTable("Boxes", userDatabase);

        TableVariable warehouseCode = new TableVariable(
                new TableVariable.Builder()
                        .userTable(warehouseTable)
                        .variableName("Code")
                        .variableType("number")
                        .pkFlag(true));

        when(userDatabaseRepository.findById(userDatabase.getId())).thenReturn(Optional.of(userDatabase));
        when(userTableRepository.findByUserDatabase(userDatabase)).thenReturn(List.of(warehouseTable, boxesTable));
        when(tableVariablesRepository.findAllByUserTable(warehouseTable)).thenReturn(List.of(warehouseCode));
        when(tableVariablesRepository.findAllByUserTable(boxesTable)).thenReturn(List.of());


        Map<String, Object> expectedSchema = new HashMap<>();
        List<List<Object>> columnNames = new ArrayList<>();
        columnNames.add(Arrays.asList(-1, "*"));
        columnNames.add(Arrays.asList(0, "Code"));

        expectedSchema.put("column_names_original", columnNames);

        List<String> columnTypes = List.of("text", "number");
        expectedSchema.put("column_types", columnTypes);

        List<String> tableNames = List.of("Warehouses", "Boxes");
        expectedSchema.put("table_names_original", tableNames);

        List<List<Integer>> foreignKeys = List.of();
        expectedSchema.put("foreign_keys", foreignKeys);

        List<Integer> primaryKeys = List.of(1);
        expectedSchema.put("primary_keys", primaryKeys);

        expectedSchema.put("db_id", "warehouse_1");

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedSchema);

        assertEquals(expectedJson, schemaModelConstructionService.constructSchema(userDatabase.getId()).toSchemaJson());
    }
}