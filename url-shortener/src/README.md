# URL Shortener Service


![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7.0-red)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20RDS%20%7C%20ElastiCache-yellow)

A high-performance distributed URL shortener service built with Java and Spring Boot.
Handles ~100K requests/day with <50ms redirect latency using a two-tier caching strategy.

---

## Architecture

Client → Spring Boot API → Redis L1 Cache (hit ~95%)
↓ (cache miss)
MySQL L2 (source of truth)
## Key Design Decisions

- **Base62 encoding** over DB auto-increment ID → 62^7 = 3.5 trillion unique URLs, URL-safe characters
- **302 redirect** over 301 → forces every redirect through server for click tracking
- **Cache-aside pattern** → Redis checked first, MySQL fallback, Redis repopulated on miss
- **Stateless service** → no session state, horizontal scaling ready
- **HikariCP pool size 10** → handles request bursts without overwhelming MySQL

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 |
| Database | MySQL 8.0 (Railway) |
| Cache | Redis 7.0 (Upstash) |
| ORM | Spring Data JPA + Hibernate |
| Build | Maven |
| Deployment | AWS EC2 + RDS + ElastiCache |

---

## API Reference

### Shorten a URL

POST /api/shorten
Content-Type: application/json
{
"originalUrl": "https://www.example.com"
}

Response:
```json
{
  "shortCode": "e",
  "shortUrl": "http://localhost:8080/e",
  "originalUrl": "https://www.example.com",
  "createdAt": "2026-04-19T09:25:17",
  "clickCount": 0
}
```

### Redirect

GET /{shortCode}
→ 302 redirect to original URL

### Health Check

GET /actuator/health

---

## Performance

- Redirect latency: **<50ms** (Redis cache hit)
- Throughput: **~100K requests/day**
- Cache hit rate: **~95%** (24h TTL)
- DB index on `shortCode` column → O(log n) lookup

---

## Local Setup

### Prerequisites
- Java 17
- Maven 3.8+
- MySQL 8.0 (or Railway free tier)
- Redis 7.0 (or Upstash free tier)

### Run
```bash
git clone https://github.com/codorhythm/url-shortener.git
cd url-shortener
# Add your DB credentials to application.yml
mvn spring-boot:run
```

---

## Screenshots

![app_start.PNG](../docs/screenshots/app_start.PNG)
![get_req.PNG](../docs/screenshots/get_req.PNG)
![live_railway.PNG](../docs/screenshots/live_railway.PNG)
![Post_req.PNG](../docs/screenshots/Post_req.PNG)
![redirect_t_google.PNG](../docs/screenshots/redirect_t_google.PNG)
![redirect_to_google.PNG](../docs/screenshots/redirect_to_google.PNG)
![redis_with cmds prcsd.PNG](../docs/screenshots/redis_with%20cmds%20prcsd.PNG)

---

## Project Structure

src/main/java/com/shortener/
├── config/          # Redis configuration
├── controller/      # REST endpoints
├── domain/          # JPA entity + DTOs
├── exception/       # Global exception handling
├── repository/      # Spring Data JPA repository
├── service/         # Business logic + caching
└── util/            # Base62 encoder