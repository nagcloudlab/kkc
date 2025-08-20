package com.example.hello_service;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HelloCountRepository extends JpaRepository<HelloCount, String> {
    // This interface will automatically provide CRUD operations for HelloCount
    // entities
    // No additional methods are needed unless specific queries are required
}
