package com.cramer.dto;

import java.util.List;

/**
 * Admin User List Response - Response cho API danh sách users
 * 
 * Chứa danh sách users và thông tin phân trang.
 */
public class AdminUserListResponse {
    
    private List<AdminUserDTO> users;
    private int currentPage;
    private int pageSize;
    private long totalItems;
    private int totalPages;
    
    // Constructors
    public AdminUserListResponse() {}
    
    public AdminUserListResponse(List<AdminUserDTO> users, int currentPage, int pageSize, long totalItems) {
        this.users = users;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }
    
    // Getters and Setters
    public List<AdminUserDTO> getUsers() {
        return users;
    }
    
    public void setUsers(List<AdminUserDTO> users) {
        this.users = users;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public long getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
