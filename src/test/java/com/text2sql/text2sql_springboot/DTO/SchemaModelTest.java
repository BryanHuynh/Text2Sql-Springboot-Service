package com.text2sql.text2sql_springboot.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SchemaModelTest {


    @Test
    void shouldReturnValidModel_toSchemaMap_WhenDataComplete() throws JsonProcessingException {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addTable("t2");
        builder.addColumn("t1", "t1c1", "text");
        builder.addColumn("t1", "t1c2", "number");

        builder.addColumn("t2", "t2c1", "text");
        builder.addColumn("t2", "t2c2", "number");
        builder.addColumn("t2", "t2c3", "date");

        builder.addPrimaryKey("t1", "t1c1");
        builder.addPrimaryKey("t2", "t2c1");

        builder.addForeignKey("t1", "t1c2", "t2", "t2c3");

        builder.setDbId("db1");

        SchemaModel schemaModel = builder.build();

        Map<String, Object> expectedSchema = new HashMap<>();
        List<List<Object>> columnNames = new ArrayList<>();
        columnNames.add(Arrays.asList(-1, "*"));
        columnNames.add(Arrays.asList(0, "t1c1"));
        columnNames.add(Arrays.asList(0, "t1c2"));
        columnNames.add(Arrays.asList(1, "t2c1"));
        columnNames.add(Arrays.asList(1, "t2c2"));
        columnNames.add(Arrays.asList(1, "t2c3"));

        expectedSchema.put("column_names_original", columnNames);

        List<String> columnTypes = List.of("text", "text", "number", "text", "number", "date");
        expectedSchema.put("column_types", columnTypes);

        List<String> tableNames = List.of("t1", "t2");
        expectedSchema.put("table_names_original", tableNames);

        List<List<Integer>> foreignKeys = List.of(List.of(5, 2));
        expectedSchema.put("foreign_keys", foreignKeys);

        List<Integer> primaryKeys = List.of(1, 3);
        expectedSchema.put("primary_keys", primaryKeys);

        expectedSchema.put("db_id", "db1");

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedSchema);

        assertEquals(expectedJson, schemaModel.toSchemaJson());
    }

    @Test
    void shouldReturnValidModel_toSchemaMap_WhenNoPrimaryKeys() throws JsonProcessingException {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addTable("t2");
        builder.addColumn("t1", "t1c1", "text");
        builder.addColumn("t1", "t1c2", "number");

        builder.addColumn("t2", "t2c1", "text");
        builder.addColumn("t2", "t2c2", "number");
        builder.addColumn("t2", "t2c3", "date");

        builder.addForeignKey("t1", "t1c2", "t2", "t2c3");

        builder.setDbId("db1");

        SchemaModel schemaModel = builder.build();

        Map<String, Object> expectedSchema = new HashMap<>();
        List<List<Object>> columnNames = new ArrayList<>();
        columnNames.add(Arrays.asList(-1, "*"));
        columnNames.add(Arrays.asList(0, "t1c1"));
        columnNames.add(Arrays.asList(0, "t1c2"));
        columnNames.add(Arrays.asList(1, "t2c1"));
        columnNames.add(Arrays.asList(1, "t2c2"));
        columnNames.add(Arrays.asList(1, "t2c3"));

        expectedSchema.put("column_names_original", columnNames);

        List<String> columnTypes = List.of("text", "text", "number", "text", "number", "date");
        expectedSchema.put("column_types", columnTypes);

        List<String> tableNames = List.of("t1", "t2");
        expectedSchema.put("table_names_original", tableNames);

        List<List<Integer>> foreignKeys = List.of(List.of(5, 2));
        expectedSchema.put("foreign_keys", foreignKeys);

        List<Integer> primaryKeys = List.of();
        expectedSchema.put("primary_keys", primaryKeys);

        expectedSchema.put("db_id", "db1");

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedSchema);

        assertEquals(expectedJson, schemaModel.toSchemaJson());
    }

    @Test
    void shouldReturnValidModel_toSchemaMap_WhenNoForeignKeys() throws JsonProcessingException {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addTable("t2");
        builder.addColumn("t1", "t1c1", "text");
        builder.addColumn("t1", "t1c2", "number");

        builder.addColumn("t2", "t2c1", "text");
        builder.addColumn("t2", "t2c2", "number");
        builder.addColumn("t2", "t2c3", "date");

        builder.addPrimaryKey("t1", "t1c1");
        builder.addPrimaryKey("t2", "t2c1");

        builder.setDbId("db1");

        SchemaModel schemaModel = builder.build();

        Map<String, Object> expectedSchema = new HashMap<>();
        List<List<Object>> columnNames = new ArrayList<>();
        columnNames.add(Arrays.asList(-1, "*"));
        columnNames.add(Arrays.asList(0, "t1c1"));
        columnNames.add(Arrays.asList(0, "t1c2"));
        columnNames.add(Arrays.asList(1, "t2c1"));
        columnNames.add(Arrays.asList(1, "t2c2"));
        columnNames.add(Arrays.asList(1, "t2c3"));

        expectedSchema.put("column_names_original", columnNames);

        List<String> columnTypes = List.of("text", "text", "number", "text", "number", "date");
        expectedSchema.put("column_types", columnTypes);

        List<String> tableNames = List.of("t1", "t2");
        expectedSchema.put("table_names_original", tableNames);

        List<List<Integer>> foreignKeys = List.of();
        expectedSchema.put("foreign_keys", foreignKeys);

        List<Integer> primaryKeys = List.of(1, 3);
        expectedSchema.put("primary_keys", primaryKeys);

        expectedSchema.put("db_id", "db1");

        ObjectMapper mapper = new ObjectMapper();
        String expectedJson = mapper.writeValueAsString(expectedSchema);

        assertEquals(expectedJson, schemaModel.toSchemaJson());
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingColumnWithNoTable() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        assertThrows(ResponseStatusException.class, () -> builder.addColumn("t1", "t1c1", "text"));
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingPrimaryKeyWithNoColumn() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        assertThrows(ResponseStatusException.class, () -> builder.addPrimaryKey("t1", "t1c1"));
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingForeignKeyRefColumnMissing() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addTable("t2");
        builder.addColumn("t1", "t1c1", "text");
        assertThrows(ResponseStatusException.class, () -> builder.addForeignKey("t1", "t1c1", "t2", "t2c1"));
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingForeignKeyRefTableMissing() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addColumn("t1", "t1c1", "text");
        assertThrows(ResponseStatusException.class, () -> builder.addForeignKey("t1", "t1c1", "t2", "t2c1"));
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingForeignKeyPrimaryColumnMissing() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t1");
        builder.addTable("t2");
        builder.addColumn("t2", "t2c1", "text");
        assertThrows(ResponseStatusException.class, () -> builder.addForeignKey("t1", "t1c1", "t2", "t2c1"));
    }

    @Test
    void shouldReturnException_BuilderConstruct_WhenAddingForeignKeyPrimaryTableMissing() {
        SchemaModel.Builder builder = new SchemaModel.Builder();
        builder.addTable("t2");
        builder.addColumn("t2", "t2c1", "text");
        assertThrows(ResponseStatusException.class, () -> builder.addForeignKey("t1", "t1c1", "t2", "t2c1"));
    }

}