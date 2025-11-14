package com.text2sql.text2sql_springboot.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

public class SchemaModel {
    private final String dbId;
    private final List<String> tableNames;
    private final Map<String, List<Map.Entry<String, String>>> tableColumns;
    private final List<Map.Entry<String, String>> primaryKeys;
    private final List<Map.Entry<Map.Entry<String, String>, Map.Entry<String, String>>> foreignKeys;


    public SchemaModel(Builder builder) {
        this.dbId = builder.dbId;
        this.tableNames = builder.getTableNames();
        this.tableColumns = builder.getTableColumnNamesAndType();
        this.primaryKeys = builder.getPrimaryKeys();
        this.foreignKeys = builder.getForeignKeys();
    }

    public Map<String, Object> toSchemaMap() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("table_names_original", tableNames);

        List<List<Object>> columnNamesOriginal = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();

        for (String tableName : tableNames) {
            List<Map.Entry<String, String>> columns = tableColumns.get(tableName);
            for (Map.Entry<String, String> column : columns) {
                columnNamesOriginal.add(Arrays.asList(tableName, column.getKey()));
                columnTypes.add(column.getValue());
            }
        }


        List<Integer> primaryKeyIndexes = primaryKeys
                .stream()
                .map(pk -> findColumnIndex(columnNamesOriginal, pk.getKey(), pk.getValue()) + 1)
                .toList();
        schema.put("primary_keys", primaryKeyIndexes);

        List<List<Integer>> foreignKeyIndexes = foreignKeys
                .stream()
                .map(fk -> {
                    // adding 1 for -1 index
                    int fromIndex = findColumnIndex(columnNamesOriginal, fk.getKey().getKey(), fk.getKey().getValue()) + 1;
                    int toIndex = findColumnIndex(columnNamesOriginal, fk.getValue().getKey(), fk.getValue().getValue()) + 1;
                    return Arrays.asList(toIndex, fromIndex);
                }).toList();
        schema.put("foreign_keys", foreignKeyIndexes);

        Map<String, Integer> tableNameIndexes = new HashMap<>();
        for (int i = 0; i < tableNames.size(); i++) {
            tableNameIndexes.put(tableNames.get(i), i);
        }
        for (List<Object> column : columnNamesOriginal) {
            String tableName = (String) column.get(0);
            int index = tableNameIndexes.get(tableName);
            String value = (String) column.get(1);
            column.set(0, index);
            column.set(1, value);
        }
        columnNamesOriginal.add(0, Arrays.asList(-1, "*"));
        columnTypes.add(0, "text");


        schema.put("column_types", columnTypes);

        schema.put("column_names_original", columnNamesOriginal);
        schema.put("db_id", dbId);

        return schema;
    }


    public String toSchemaJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(toSchemaMap());
    }

    private int findColumnIndex(List<List<Object>> columnNamesOriginal, String tableName, String columnName) {
        for (int i = 0; i < columnNamesOriginal.size(); i++) {
            if (columnNamesOriginal.get(i).get(0).equals(tableName) && columnNamesOriginal.get(i).get(1).equals(columnName)) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Column name not found");
    }


    public static class Builder {


        private String dbId;
        private final Map<String, List<Map.Entry<String, String>>> tableColumnNamesAndType = new HashMap<>();
        private final List<String> tableNames = new ArrayList<>();
        private final List<Map.Entry<String, String>> primaryKeys = new ArrayList<>();
        private final List<Map.Entry<Map.Entry<String, String>, Map.Entry<String, String>>> foreignKeys = new ArrayList<>();

        public Builder addTable(String tableName) {
            tableNames.add(tableName);
            tableColumnNamesAndType.put(tableName, new ArrayList<>());
            return this;
        }

        public Builder addColumn(String tableName, String columnName, String columnType) throws ResponseStatusException {
            if (tableNames.contains(tableName) && tableColumnNamesAndType.containsKey(tableName)) {
                tableColumnNamesAndType.get(tableName).add(new AbstractMap.SimpleEntry<>(columnName, columnType));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table Not Found");
            }
            return this;
        }

        public Builder addPrimaryKey(String tableName, String columnName) throws ResponseStatusException {
            if (
                    tableNames.contains(tableName) &&
                            tableColumnNamesAndType.containsKey(tableName) &&
                            tableColumnNamesAndType.get(tableName).stream().anyMatch(entry -> entry.getKey().equals(columnName))
            ) {

                primaryKeys.add(new AbstractMap.SimpleEntry<>(tableName, columnName));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table or Column Not Found");
            }
            return this;
        }

        public Builder addForeignKey(String primaryTableName, String primaryColumnName, String refTableName, String refColumnName) throws ResponseStatusException {
            if (
                    tableNames.contains(primaryTableName) && tableColumnNamesAndType.containsKey(primaryTableName) &&
                            tableNames.contains(refTableName) && tableColumnNamesAndType.containsKey(refTableName) &&
                            tableColumnNamesAndType.get(primaryTableName).stream().anyMatch(entry -> entry.getKey().equals(primaryColumnName)) &&
                            tableColumnNamesAndType.get(refTableName).stream().anyMatch(entry -> entry.getKey().equals(refColumnName))
            ) {
                foreignKeys.add(new AbstractMap.SimpleEntry<>(
                        new AbstractMap.SimpleEntry<>(primaryTableName, primaryColumnName),
                        new AbstractMap.SimpleEntry<>(refTableName, refColumnName)
                ));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table or Column Not Found");
            }
            return this;
        }

        public SchemaModel build() {
            return new SchemaModel(this);
        }

        public List<Map.Entry<Map.Entry<String, String>, Map.Entry<String, String>>> getForeignKeys() {
            return foreignKeys;
        }

        public List<Map.Entry<String, String>> getPrimaryKeys() {
            return primaryKeys;
        }

        public List<String> getTableNames() {
            return tableNames;
        }

        public Map<String, List<Map.Entry<String, String>>> getTableColumnNamesAndType() {
            return tableColumnNamesAndType;
        }

        public String getDbId() {
            return dbId;
        }

        public void setDbId(String dbId) {
            this.dbId = dbId;
        }


    }
}

