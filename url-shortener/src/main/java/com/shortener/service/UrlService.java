package com.shortener.service;

import com.shortener.domain.Url;
import com.shortener.domain.UrlRequestDto;
import com.shortener.domain.UrlResponseDto;
import com.shortener.exception.UrlNotFoundException;
import com.shortener.repository.UrlRepository;
import com.shortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.cache-ttl-hours}")
    private long cacheTtlHours;

    private static final String CACHE_PREFIX = "url:";
    private static final double BETA = 1.0;

    @Transactional
    public UrlResponseDto shortenUrl(UrlRequestDto request) {
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode("temp")
                .build();

        Url saved = urlRepository.save(url);

        String shortCode = base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        urlRepository.save(saved);

        redisTemplate.opsForValue().set(
                CACHE_PREFIX + shortCode,
                saved.getOriginalUrl(),
                Duration.ofHours(cacheTtlHours)
        );

        log.info("Shortened URL: {} -> {}", request.getOriginalUrl(), shortCode);
        return buildResponse(saved);
    }

    @Transactional
    public String resolveUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        Long ttlSeconds = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);

        if (cached != null && ttlSeconds != null && ttlSeconds > 0) {
            double refreshProbability = Math.exp(-BETA * ttlSeconds / (cacheTtlHours * 3600.0));
            if (Math.random() > refreshProbability) {
                log.debug("Cache HIT for shortCode: {}", shortCode);
                incrementClickCountAsync(shortCode);
                return cached.toString();
            }
            log.debug("Probabilistic early refresh triggered for shortCode: {}", shortCode);
        }

        log.debug("Cache MISS for shortCode: {}", shortCode);
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        redisTemplate.opsForValue().set(
                cacheKey,
                url.getOriginalUrl(),
                Duration.ofHours(cacheTtlHours)
        );

        incrementClickCountAsync(shortCode);
        return url.getOriginalUrl();
    }

    public UrlResponseDto getUrlStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return buildResponse(url);
    }

    private void incrementClickCountAsync(String shortCode) {
        try {
            urlRepository.incrementClickCount(shortCode);
        } catch (Exception e) {
            log.warn("Failed to increment click count for: {}", shortCode);
        }
    }

    private UrlResponseDto buildResponse(Url url) {
        return UrlResponseDto.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .clickCount(url.getClickCount())
                .build();
    }
}