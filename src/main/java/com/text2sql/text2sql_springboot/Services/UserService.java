package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.CreateUpdateUserRequest;
import com.text2sql.text2sql_springboot.DTO.UserDto;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UserDto findById() {
        var u = repo.findById(AppContext.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto(u.getId(), u.getEmail());
    }

    @Transactional
    public UserDto create(CreateUpdateUserRequest request) {
        if (repo.existsById(request.id()) || repo.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or ID already exists");
        }
        var saved = repo.save(new UserDetail(request.id(), request.email()));
        return new UserDto(saved.getId(), saved.getEmail());
    }


    @Transactional
    public void delete() {
        String id = AppContext.getCurrentUserId();
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id + " not found");
        }
        repo.deleteById(id);
    }

}
