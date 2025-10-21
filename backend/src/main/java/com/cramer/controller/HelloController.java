package com.cramer.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong from Cramer backend") ;
    }

    @Autowired
    private DataSource dataSource;

    @GetMapping("/db-check")
    public Map<String, Object> dbCheck() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int v = rs.getInt(1);
                return Map.of("db", "ok", "value", v);
            } else {
                return Map.of("db", "no-result");
            }
        } catch (SQLException e) {
            return Map.of("db", "error", "message", e.getMessage());
        }
    }
}
