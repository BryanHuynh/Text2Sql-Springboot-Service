package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserDetails, String> {
    Optional<UserDetails> findByEmail(String email);

    boolean existsByEmail(String email);


}
