package com.text2sql.text2sql_springboot.Repositories;

import com.text2sql.text2sql_springboot.Entities.UserDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repo;

    @Test
    void save_and_findById() {
        var saved = repo.save(new UserDetail("123", "test@email.me"));
        assertThat(saved.getId()).isNotNull();

        var found = repo.findById(saved.getId());
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get()).isEqualTo(saved);
    }

    @Test
    void save_and_delete() {
        var saved = repo.save(new UserDetail("123", "test@email.me"));
        assertThat(saved.getId()).isNotNull();
        repo.deleteById(saved.getId());
        var found = repo.findById(saved.getId());
        assertThat(found.isPresent()).isFalse();
    }
}