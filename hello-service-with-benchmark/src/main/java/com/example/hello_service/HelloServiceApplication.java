package com.example.hello_service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HelloServiceApplication {

	private AtomicInteger reqCount = new AtomicInteger(0);

	// String helloCount = "0"; // string data-structure to hold the count

	private final HelloCountRepository helloCountRepository;
	private final RedisTemplate<String, Long> redisTemplate;
	private final RedisTemplate<String, Long> keydbTemplate;

	public HelloServiceApplication(HelloCountRepository helloCountRepository,
			RedisTemplate<String, Long> redisTemplate, RedisTemplate<String, Long> keydbTemplate) {
		this.helloCountRepository = helloCountRepository;
		this.redisTemplate = redisTemplate;
		this.keydbTemplate = keydbTemplate;
	}

	@GetMapping("/hello/postgres")
	public synchronized String helloPostgres() {
		// Increment the request count
		int reCount = reqCount.incrementAndGet();
		System.out.println("Request count: " + reCount);

		// int count = Integer.parseInt(helloCount);
		// count++;
		// helloCount = String.valueOf(count);
		// return "Hello, World! This is message number: " + helloCount;

		HelloCount helloCount = helloCountRepository.findById("user1").orElse(new HelloCount(0));
		int count = helloCount.getCount();
		count++;
		helloCount.setCount(count);
		helloCount.setUser("user1"); // Set the user ID
		helloCountRepository.save(helloCount);

		return "Hello, World! This is message number: " + count;

	}

	@GetMapping("/hello/redis")
	public String helloRedis() {
		// Increment the request count
		int reCount = reqCount.incrementAndGet();
		System.out.println("Request count: " + reCount);

		// Use Redis to store and retrieve the count
		String key = "user1:hello:count"; // Key for Redis
		// Long count = redisTemplate.opsForValue().get(key); // GET user1:hello:count
		// if (count == null) {
		// count = 0L; // Initialize if not present
		// }
		// count++;
		// redisTemplate.opsForValue().set(key, count); // SET user1:hello:count n

		Long count = redisTemplate.opsForValue().increment(key, 1); // INCR user1:hello:count
		if (count == null) {
			count = 0L; // Initialize if not present
		}
		// Return the message with the count
		return "Hello, World! This is message number: " + count;
	}

	@GetMapping("/hello/keydb")
	public String hellokeydb() {
		// Increment the request count
		int reCount = reqCount.incrementAndGet();
		System.out.println("Request count: " + reCount);

		// Use Redis to store and retrieve the count
		String key = "user1:hello:count"; // Key for Redis
		// Long count = redisTemplate.opsForValue().get(key);
		// if (count == null) {
		// count = 0L; // Initialize if not present
		// }
		// count++;
		// redisTemplate.opsForValue().set(key, count);

		Long count = keydbTemplate.opsForValue().increment(key, 1);
		if (count == null) {
			count = 0L; // Initialize if not present
		}
		// Return the message with the count
		return "Hello, World! This is message number: " + count;
	}


	public static void main(String[] args) {
		SpringApplication.run(HelloServiceApplication.class, args);
	}

}
