package com.cramer.dto;

import lombok.Data;

public class CheckEmailRequest {
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
