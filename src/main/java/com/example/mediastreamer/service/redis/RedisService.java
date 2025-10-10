package com.example.mediastreamer.service.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Optional;

@Service
public class RedisService {

    private final JedisPool jedisPool = new JedisPool("localhost", 6379);

    public Optional<String> get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return Optional.ofNullable(jedis.get(key));
        }
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    @PreDestroy
    public void shutdown() {
        jedisPool.close();
    }
}
