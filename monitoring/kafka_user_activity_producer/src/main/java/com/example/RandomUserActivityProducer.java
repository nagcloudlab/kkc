package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

class UserActivity {
    private String user_id;
    private String activity_type; // login | transfer | logout
    private String timestamp; // epoch millis as string
    private String session_id; // same for login/transfer/logout in one flow
    private BigDecimal amount; // only for transfer

    public UserActivity(String user_id, String activity_type, String timestamp, String session_id, BigDecimal amount) {
        this.user_id = user_id;
        this.activity_type = activity_type;
        this.timestamp = timestamp;
        this.session_id = session_id;
        this.amount = amount;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getActivity_type() {
        return activity_type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSession_id() {
        return session_id;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

public class RandomUserActivityProducer {

    private static final String TOPIC = "user-activities";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int USER_COUNT = 10;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // reliability & throughput tweaks
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768); // 32KB
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        return new KafkaProducer<>(props);
    }

    private static String toJson(UserActivity ua) {
        try {
            return MAPPER.writeValueAsString(ua);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize UserActivity", e);
        }
    }

    private static void sleepRandom(int minMillis, int maxMillis, Random rnd) {
        int delay = minMillis + rnd.nextInt(Math.max(1, maxMillis - minMillis + 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static BigDecimal randomAmount(Random rnd) {
        // 100.00 to 5000.00
        double v = 100 + (rnd.nextDouble() * 4900);
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static void logWithEmoji(UserActivity ua) {
        String emoji;
        switch (ua.getActivity_type()) {
            case "login":
                emoji = "🔐";
                break;
            case "transfer":
                emoji = "💸";
                break;
            case "logout":
                emoji = "🚪";
                break;
            default:
                emoji = "📝";
        }
        System.out.printf(
                "%s  user=%s 👤  session=%s 🧾  type=%s  ts=%s ⏱%s%s%n",
                emoji,
                ua.getUser_id(),
                ua.getSession_id(),
                ua.getActivity_type(),
                ua.getTimestamp(),
                ua.getAmount() != null ? "  amount=" + ua.getAmount() : "",
                "");
    }

    public static void main(String[] args) {
        KafkaProducer<String, String> producer = createKafkaProducer();
        ExecutorService pool = Executors.newFixedThreadPool(USER_COUNT);
        List<Future<?>> futures = new ArrayList<>();
        CountDownLatch stopSignal = new CountDownLatch(1);

        // graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚙️  Shutdown requested. Draining...");
            stopSignal.countDown();
            pool.shutdownNow();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            producer.flush();
            producer.close();
            System.out.println("✅ Clean shutdown.");
        }));

        // users 1 to USER_COUNT
        for (int u = 1; u <= USER_COUNT; u++) {
            final String userId = String.valueOf(u);
            futures.add(pool.submit(() -> {
                Random rnd = new Random();
                while (stopSignal.getCount() > 0) {
                    String sessionId = UUID.randomUUID().toString();

                    // LOGIN
                    UserActivity login = new UserActivity(
                            userId, "login", String.valueOf(Instant.now().toEpochMilli()), sessionId, null);
                    logWithEmoji(login);
                    send(producer, userId, login);

                    // random think time 200–1500 ms
                    sleepRandom(200, 1500, rnd);

                    // TRANSFER
                    UserActivity transfer = new UserActivity(
                            userId, "transfer", String.valueOf(Instant.now().toEpochMilli()), sessionId,
                            randomAmount(rnd));
                    logWithEmoji(transfer);
                    send(producer, userId, transfer);

                    // random think time 300–3000 ms
                    sleepRandom(300, 3000, rnd);

                    // LOGOUT
                    UserActivity logout = new UserActivity(
                            userId, "logout", String.valueOf(Instant.now().toEpochMilli()), sessionId, null);
                    logWithEmoji(logout);
                    send(producer, userId, logout);

                    // gap before next session 500–4000 ms
                    sleepRandom(500, 4000, rnd);
                }
            }));
        }

        // keep main alive
        try {
            for (Future<?> f : futures)
                f.get();
        } catch (CancellationException | InterruptedException ignored) {
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void send(KafkaProducer<String, String> producer, String keyUserId, UserActivity ua) {
        String payload = toJson(ua);
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, keyUserId, payload);

        try {
            RecordMetadata m = producer.send(record).get(); // sync for demo; swap to async in prod
            System.out.printf("📤 sent → topic=%s partition=%d offset=%d key=%s%n",
                    m.topic(), m.partition(), m.offset(), keyUserId);
        } catch (Exception e) {
            System.err.printf("❌ send failed for user=%s: %s%n", keyUserId, e.getMessage());
        }
    }
}
