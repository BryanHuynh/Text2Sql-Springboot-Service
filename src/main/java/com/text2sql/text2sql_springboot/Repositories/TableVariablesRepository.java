package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.TableVariable;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TableVariablesRepository extends JpaRepository<TableVariable, UUID> {
    public List<TableVariable> findAllByUserTable(UserTable req);
}
