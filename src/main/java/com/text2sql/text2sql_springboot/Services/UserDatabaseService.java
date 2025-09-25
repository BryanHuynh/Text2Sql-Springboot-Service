package com.text2sql.text2sql_springboot.Services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.text2sql.text2sql_springboot.DTO.CreateUserDatabaseRequest;
import com.text2sql.text2sql_springboot.DTO.UserDatabaseDto;
import com.text2sql.text2sql_springboot.Entities.UserDetail;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserRepository;
import com.text2sql.text2sql_springboot.security.AppContext;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class UserDatabaseService {
    private final UserDatabaseRepository userDatabaseRepository;
    private final UserRepository userRepository;

    public UserDatabaseService(UserDatabaseRepository userFileRepository, UserRepository userRepository) {
        this.userDatabaseRepository = userFileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<UserDatabaseDto> getAllUserDatabases() {
        return userDatabaseRepository.findByUserId(AppContext.getCurrentUserId())
                .stream()
                .map((file) ->
                        new UserDatabaseDto(file.getDatabaseName(), file.getId())
                ).toList();
    }

    @Transactional
    public UserDatabaseDto create(CreateUserDatabaseRequest createUserFileRequest) {
        UserDetail user = userRepository.getReferenceById(AppContext.getCurrentUserId());
        var saved = userDatabaseRepository.save(new UserDatabase(createUserFileRequest.filename(), user));
        return new UserDatabaseDto(saved.getDatabaseName(), saved.getId());
    }

    @Transactional
    public UserDatabaseDto update(CreateUserDatabaseRequest req) throws ResponseStatusException {
        UUID databaseId = req.database_id().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        UserDatabase file = userDatabaseRepository.findById(databaseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!file.getUser().getId().equals(AppContext.getCurrentUserId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        file.setDatabaseName(req.filename());
        userDatabaseRepository.save(file);
        return new UserDatabaseDto(file.getDatabaseName(), file.getId());
    }

    @Transactional
    public void delete(UUID id) {
        UserDatabase file = userDatabaseRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userDatabaseRepository.delete(file);
    }

}
