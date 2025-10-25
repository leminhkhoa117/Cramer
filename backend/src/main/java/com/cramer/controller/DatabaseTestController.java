package com.cramer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for testing database connectivity with Supabase.
 * Provides comprehensive diagnostics and logging.
 */
@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTestController.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Comprehensive database connection test
     */
    @GetMapping("/db-full")
    public Map<String, Object> testDatabaseFull() {
        log.info("=================================================");
        log.info("Starting comprehensive database connection test");
        log.info("=================================================");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("testName", "Comprehensive Database Test");

        // Test 1: Basic Connection
        Map<String, Object> basicTest = testBasicConnection();
        result.put("basicConnection", basicTest);
        log.info("Basic Connection Test: {}", basicTest.get("status"));

        // Test 2: Database Metadata
        Map<String, Object> metadataTest = testDatabaseMetadata();
        result.put("databaseMetadata", metadataTest);
        log.info("Database Metadata Test: {}", metadataTest.get("status"));

        // Test 3: Check Tables
        Map<String, Object> tablesTest = testTablesExist();
        result.put("tablesCheck", tablesTest);
        log.info("Tables Check: {}", tablesTest.get("status"));

        // Test 4: Query Test
        Map<String, Object> queryTest = testSimpleQuery();
        result.put("queryTest", queryTest);
        log.info("Query Test: {}", queryTest.get("status"));

        // Test 5: Count Records
        Map<String, Object> countTest = testCountRecords();
        result.put("recordCounts", countTest);
        log.info("Record Count Test: {}", countTest.get("status"));

        // Overall Status
        boolean allPassed = 
            "SUCCESS".equals(basicTest.get("status")) &&
            "SUCCESS".equals(metadataTest.get("status")) &&
            "SUCCESS".equals(tablesTest.get("status")) &&
            "SUCCESS".equals(queryTest.get("status")) &&
            "SUCCESS".equals(countTest.get("status"));

        result.put("overallStatus", allPassed ? "ALL TESTS PASSED ✅" : "SOME TESTS FAILED ❌");
        
        log.info("=================================================");
        log.info("Overall Test Result: {}", result.get("overallStatus"));
        log.info("=================================================");

        return result;
    }

    /**
     * Test 1: Basic database connection
     */
    private Map<String, Object> testBasicConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            result.put("status", "SUCCESS");
            result.put("connected", true);
            result.put("connectionClass", conn.getClass().getName());
            result.put("autoCommit", conn.getAutoCommit());
            result.put("readOnly", conn.isReadOnly());
            result.put("transactionIsolation", conn.getTransactionIsolation());
            
            log.info("✓ Basic connection successful");
            log.info("  Connection class: {}", conn.getClass().getName());
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Basic connection failed: {}", e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Test 2: Database metadata
     */
    private Map<String, Object> testDatabaseMetadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            
            result.put("status", "SUCCESS");
            result.put("databaseProductName", metadata.getDatabaseProductName());
            result.put("databaseProductVersion", metadata.getDatabaseProductVersion());
            result.put("databaseMajorVersion", metadata.getDatabaseMajorVersion());
            result.put("databaseMinorVersion", metadata.getDatabaseMinorVersion());
            result.put("driverName", metadata.getDriverName());
            result.put("driverVersion", metadata.getDriverVersion());
            result.put("jdbcUrl", metadata.getURL());
            result.put("username", metadata.getUserName());
            result.put("supportsTransactions", metadata.supportsTransactions());
            
            log.info("✓ Database metadata retrieved");
            log.info("  Database: {} {}", 
                metadata.getDatabaseProductName(), 
                metadata.getDatabaseProductVersion());
            log.info("  JDBC URL: {}", metadata.getURL());
            log.info("  Username: {}", metadata.getUserName());
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Metadata retrieval failed: {}", e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Test 3: Check if required tables exist
     */
    private Map<String, Object> testTablesExist() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Boolean> tables = new LinkedHashMap<>();
        String[] requiredTables = {"profiles", "sections", "questions", "user_answers"};
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            
            for (String tableName : requiredTables) {
                try (ResultSet rs = metadata.getTables(null, "public", tableName, new String[]{"TABLE"})) {
                    boolean exists = rs.next();
                    tables.put(tableName, exists);
                    
                    if (exists) {
                        log.info("  ✓ Table '{}' exists", tableName);
                    } else {
                        log.warn("  ✗ Table '{}' NOT found", tableName);
                    }
                }
            }
            
            result.put("status", "SUCCESS");
            result.put("tables", tables);
            result.put("allTablesExist", tables.values().stream().allMatch(Boolean::booleanValue));
            
            log.info("✓ Table existence check completed");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Table check failed: {}", e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Test 4: Simple query test
     */
    private Map<String, Object> testSimpleQuery() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 AS test_value, NOW() AS current_time")) {
            
            if (rs.next()) {
                result.put("status", "SUCCESS");
                result.put("queryResult", rs.getInt("test_value"));
                result.put("serverTime", rs.getTimestamp("current_time").toString());
                
                log.info("✓ Simple query executed successfully");
                log.info("  Server time: {}", rs.getTimestamp("current_time"));
            } else {
                result.put("status", "FAILED");
                result.put("error", "No result returned");
                log.warn("✗ Query returned no results");
            }
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Query execution failed: {}", e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Test 5: Count records in each table
     */
    private Map<String, Object> testCountRecords() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> counts = new LinkedHashMap<>();
        String[] tables = {"profiles", "sections", "questions", "user_answers"};
        
        try (Connection conn = dataSource.getConnection()) {
            for (String tableName : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM public." + tableName)) {
                    
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        counts.put(tableName, count);
                        log.info("  {} table: {} records", tableName, count);
                    }
                    
                } catch (SQLException e) {
                    counts.put(tableName, "ERROR: " + e.getMessage());
                    log.warn("  ✗ Could not count records in '{}': {}", tableName, e.getMessage());
                }
            }
            
            result.put("status", "SUCCESS");
            result.put("counts", counts);
            
            log.info("✓ Record count completed");
            
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Record count failed: {}", e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Simple ping endpoint
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        log.info("Ping endpoint called");
        return Map.of(
            "status", "OK",
            "message", "Database test controller is running",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * Test connection pool status
     */
    @GetMapping("/pool-status")
    public Map<String, Object> poolStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // Try to get connection from pool
            long startTime = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection()) {
                long endTime = System.currentTimeMillis();
                
                result.put("status", "SUCCESS");
                result.put("connectionAcquired", true);
                result.put("acquisitionTimeMs", endTime - startTime);
                result.put("connectionValid", conn.isValid(5));
                
                log.info("✓ Connection pool test passed");
                log.info("  Acquisition time: {} ms", endTime - startTime);
            }
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            log.error("✗ Connection pool test failed: {}", e.getMessage(), e);
        }
        
        return result;
    }
}
