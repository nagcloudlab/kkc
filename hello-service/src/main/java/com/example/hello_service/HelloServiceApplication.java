package com.example.hello_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

@SpringBootApplication
@RestController
public class HelloServiceApplication {

    // Appln's in-memory data
    // on-restart/crash, this will reset
    // on scale, this will not be shared
    //String hello_count = "0";

    // using redis to persist the count
    Jedis jedis = new Jedis("localhost", 6379);

    @GetMapping(
            value = "/hello"
    )
    public String hello() {
//        int count = Integer.parseInt(hello_count);
//        count++;
//        hello_count = String.valueOf(count);
//        System.out.println("Hello endpoint called " + hello_count + " times");


        String hello_count = jedis.get("hello_count");
        if (hello_count == null) {
            hello_count = "0";
        }
        int count = Integer.parseInt(hello_count);
        count++;
        hello_count = String.valueOf(count);
        jedis.set("hello_count", hello_count);

        // Return a simple greeting message
        return "Hello, World! This endpoint has been called " + hello_count + " times.";
    }

    public static void main(String[] args) {
        SpringApplication.run(HelloServiceApplication.class, args);
    }

}
