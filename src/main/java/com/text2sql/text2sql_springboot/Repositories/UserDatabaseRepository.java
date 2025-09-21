package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserDatabaseRepository extends JpaRepository<UserDatabase, UUID> {
    List<UserDatabase> findByUserId(@NotBlank String user_id);
}
