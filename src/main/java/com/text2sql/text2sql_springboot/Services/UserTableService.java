package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.DTO.UserTableDto;
import com.text2sql.text2sql_springboot.DTO.UserTableRequest;
import com.text2sql.text2sql_springboot.Entities.UserDatabase;
import com.text2sql.text2sql_springboot.Entities.UserTable;
import com.text2sql.text2sql_springboot.Repositories.UserDatabaseRepository;
import com.text2sql.text2sql_springboot.Repositories.UserTableRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserTableService {
    private final UserTableRepository userTableRepository;
    private final UserDatabaseRepository userDatabaseRepository;

    public UserTableService(UserTableRepository userTableRepository, UserDatabaseRepository userFileRepository) {
        this.userTableRepository = userTableRepository;
        this.userDatabaseRepository = userFileRepository;
    }

    @Transactional
    public List<UserTableDto> findAllByDatabaseId(UUID databaseId) {
        UserDatabase db = userDatabaseRepository.findById(databaseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var u = userTableRepository.findByUserDatabase(db);
        return new ArrayList<UserTableDto>(
                u.stream().map(
                        (table) -> new UserTableDto(table.getId(),
                                table.getTableName(),
                                table.getUserDatabase().getId()
                        )).toList());
    }

    @Transactional
    public UserTableDto create(UserTableRequest req) {
        UserDatabase file = userDatabaseRepository.findById(req.userDatabaseId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserTable saved = userTableRepository.save(new UserTable(req.tablename(), file));
        return new UserTableDto(saved.getId(), saved.getTableName(), saved.getUserDatabase().getId());
    }

    @Transactional
    public UserTableDto update(UserTableRequest req) {
        if (req.id().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id is required");
        UserTable table = userTableRepository.findById(req.id().get()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        table.setTableName(req.tablename());
        UserTable save = userTableRepository.save(table);
        return new UserTableDto(save.getId(), save.getTableName(), save.getUserDatabase().getId());
    }

    @Transactional
    public void delete(UUID id) {
        UserTable table = userTableRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userTableRepository.delete(table);
    }


}
