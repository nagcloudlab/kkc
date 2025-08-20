package com.example.hello_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hello_count")
public class HelloCount {

    @Id
    @Column(name = "hello_user", unique = true, nullable = false)
    private String user;
    @Column(name = "count", nullable = false)
    private int count;

    public HelloCount() {
    }

    public HelloCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

}
