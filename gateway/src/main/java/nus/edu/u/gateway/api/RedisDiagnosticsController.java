package nus.edu.u.gateway.api;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/diagnostics")
@Slf4j
public class RedisDiagnosticsController {

    private final ReactiveStringRedisTemplate redisTemplate;

    @GetMapping("/redis")
    public Mono<Map<String, Object>> redis() {
        String key = "gateway:diag:redis:" + UUID.randomUUID();
        String value = Instant.now().toString();

        return redisTemplate
                .opsForValue()
                .set(key, value, Duration.ofSeconds(10))
                .flatMap(
                        written ->
                                redisTemplate
                                        .opsForValue()
                                        .get(key)
                                        .map(read -> buildResult(key, written, read))
                                        .defaultIfEmpty(buildResult(key, written, null)))
                .doOnSuccess(result -> log.info("Redis diagnostics succeeded: {}", result));
    }

    @GetMapping("/satoken")
    public Mono<Map<String, Object>> saToken() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reachable", true);
        result.put("token", StpUtil.getTokenValue());

        try {
            StpUtil.checkLogin();
            result.put("loggedIn", true);
            result.put("loginId", StpUtil.getLoginIdDefaultNull());
        } catch (NotLoginException ex) {
            result.put("loggedIn", false);
            result.put("error", ex.getMessage());
        } catch (Throwable ex) {
            result.put("loggedIn", false);
            result.put("error", ex.toString());
        }

        log.info("Sa-Token diagnostics: {}", result);
        return Mono.just(result);
    }

    private Map<String, Object> buildResult(String key, Boolean written, String value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reachable", true);
        result.put("written", written);
        result.put("key", key);
        result.put("value", value);
        return result;
    }
}
