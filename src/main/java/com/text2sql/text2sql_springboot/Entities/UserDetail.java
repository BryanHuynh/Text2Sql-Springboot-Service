package com.text2sql.text2sql_springboot.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "USER_DETAILS", schema = "public")
public class UserDetail {
    @Id
    @NotBlank
    @Column(nullable = false, length = 255)
    private String id;

    @NotBlank
    @Email
    @Column(nullable = false, length = 255)
    private String email;


    public UserDetail() {
    }

    public UserDetail(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void update(UserDetail userDetails) {
        this.email = userDetails.getEmail();
    }
}
