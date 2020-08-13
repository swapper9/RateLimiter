package com.swapper.ratelimiter;

import com.google.common.util.concurrent.RateLimiter;
import com.swapper.ratelimiter.exception.RateLimitException;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Класс для контроля над количеством запросов к REST api
 * Необходимо добавить к методу контроллера аннотацию @com.swapper.ratelimiter.RateLimit(value = n),
 * где n - допустимое количество запросов в секунду.
 */
@Aspect
@Component
@Log4j2
public class RateLimiterAspect {

    private static final String[] IP_HEADER_NAMES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private static final String RATE_LIMIT_PRECONDITION_FAIL = "HttpServletRequest parameter is missing";

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void rateLimit(JoinPoint jp, RateLimit rateLimit) {
        RateLimiter limiter = limiters.computeIfAbsent(createKey(jp), createLimiter(rateLimit));
        boolean acquired = limiter.tryAcquire(0, TimeUnit.SECONDS);
        if (!acquired) {
            throw new RateLimitException("Rate limit exceeded");
        }
    }

    private Function<String, RateLimiter> createLimiter(RateLimit limit) {
        return name -> RateLimiter.create(limit.value());
    }

    private String createKey(JoinPoint jp) {
        Object[] args = jp.getArgs();
        if (args.length <= 0) {
            throw new IllegalArgumentException(RATE_LIMIT_PRECONDITION_FAIL);
        }

        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) arg;

                String ipAddress = Arrays.stream(IP_HEADER_NAMES)
                        .map(request::getHeader)
                        .filter(h -> h != null && h.length() != 0 && !"unknown".equalsIgnoreCase(h))
                        .map(h -> h.split(",")[0])
                        .reduce("", (h1, h2) -> h1 + ":" + h2);
                if (ipAddress == null) {
                    ipAddress = request.getRemoteAddr();
                }
                return ipAddress;
            }
        }
        throw new IllegalArgumentException(RATE_LIMIT_PRECONDITION_FAIL);
    }
}
