package com.text2sql.text2sql_springboot.Entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "USER_DATABASES", schema = "public")
public class UserDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL
    private UUID id;

    @Column(name = "database_name", nullable = false, length = 100)
    private String databaseName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user")
    )
    private UserDetail user;

    public UserDatabase() {
    }

    public UserDatabase(String databaseName, UserDetail user) {
        this.databaseName = databaseName;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String filename) {
        this.databaseName = filename;
    }

    public UserDetail getUser() {
        return user;
    }
}
