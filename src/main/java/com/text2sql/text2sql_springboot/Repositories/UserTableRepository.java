package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTableRepository extends JpaRepository<UserTable, UUID> {
    public List<UserTable> findByUserDatabase(UserDatabase file);
}
