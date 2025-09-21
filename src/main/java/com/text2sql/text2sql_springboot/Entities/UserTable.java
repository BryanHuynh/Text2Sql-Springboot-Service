package com.text2sql.text2sql_springboot.Entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "USER_TABLES", schema = "public")
public class UserTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private UUID id;


    @Column(name = "tablename", length = 255)
    private String tableName;

    @ManyToOne
    @JoinColumn(name = "user_database_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_database"))
    private UserDatabase userDatabase;

    public UserTable(String tablename, UserDatabase file) {
        this.tableName = tablename;
        this.userDatabase = file;
    }

    public UserTable() {

    }

    public UUID getId() {
        return id;
    }

    public UserDatabase getUserDatabase() {
        return userDatabase;
    }

    public void setUserDatabase(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


}
