package org.example.ilink.controller;

import org.example.ilink.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 用于验证 MySQL、Redis 服务连接是否正常
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 检查所有服务状态
     * GET /api/health
     */
    @GetMapping
    public Result checkAll() {
        Map<String, Object> result = new HashMap<>();
        result.put("mysql", checkMySQL());
        result.put("redis", checkRedis());
        return Result.success(result);
    }

    /**
     * 检查 MySQL 连接
     * GET /api/health/mysql
     */
    @GetMapping("/mysql")
    public Result checkMySQLApi() {
        return Result.success(checkMySQL());
    }

    /**
     * 检查 Redis 连接
     * GET /api/health/redis
     */
    @GetMapping("/redis")
    public Result checkRedisApi() {
        return Result.success(checkRedis());
    }


    private Map<String, Object> checkMySQL() {
        Map<String, Object> info = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            info.put("status", "ok");
            info.put("url", conn.getMetaData().getURL());
            info.put("valid", conn.isValid(2));
        } catch (Exception e) {
            info.put("status", "error");
            info.put("message", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> info = new HashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection().ping();
            info.put("status", "ok");
            info.put("ping", pong);
        } catch (Exception e) {
            info.put("status", "error");
            info.put("message", e.getMessage());
        }
        return info;
    }
}
