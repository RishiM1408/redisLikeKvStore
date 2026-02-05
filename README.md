<div align="center">
  <h3>Project Views</h3>
  <img src="https://komarev.com/ghpvc/?username=RishiM1408-redis-like-kv-store&label=Project%20Views&color=0e75b6&style=flat" alt="Project Views" />
</div>
# Redis-like Key-Value Store

A high-performance, Redis-compatible key-value store implemented in Java with enterprise-grade features.

## Features

- Redis protocol compatibility
- High availability with HAProxy load balancing
- Thread-safe operations
- Key expiration support
- Docker containerization
- Efficient logging with rotation
- Horizontal scalability

## Requirements

- Docker and Docker Compose
- Java 17 (for local development)
- Maven (for local development)

## Building and Running

### Using Docker Compose (Recommended)

1. Build and start the services:
```bash
docker-compose up -d
```

2. Scale the service (optional):
```bash
docker-compose up -d --scale kvstore=3
```

3. Access the service:
- Primary endpoint: localhost:6378 (HAProxy)
- Direct endpoints: 
  - localhost:6379 (kvstore1)
  - localhost:6380 (kvstore2)

### Local Development

1. Build the project:
```bash
mvn clean package
```

2. Run the server:
```bash
java -jar target/redis-like-kvstore-1.0-SNAPSHOT.jar
```

## Supported Commands

- PING - Test server connection
- SET key value [EX seconds | PX milliseconds] - Set key with optional expiration
- GET key - Get value by key
- DEL key - Delete key
- EXISTS key - Check if key exists
- EXPIRE key seconds - Set key expiration

## Configuration

### Memory Settings

Memory settings can be adjusted in docker-compose.yml:
```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx512m -XX:+UseG1GC
```

### High Availability

The system uses HAProxy for load balancing with the following features:
- Round-robin load balancing
- Health checks
- Automatic failover
- Backup server support

### Logging

Logs are stored in the /app/logs directory with automatic rotation:
- Maximum log size: 100MB
- Maximum history: 7 days
- Compression enabled

## Performance Considerations

- Uses Netty for non-blocking I/O
- Thread-safe operations with read-write locks
- G1 Garbage Collector for better latency
- Asynchronous logging
- Connection pooling in HAProxy

## Monitoring

Health checks are configured for each service and can be monitored through Docker:
```bash
docker-compose ps
docker-compose logs
```

## Client Usage Example

Using Redis CLI:
```bash
redis-cli -p 6378
> SET mykey "Hello World"
OK
> GET mykey
"Hello World"
> EXPIRE mykey 60
(integer) 1
```

## License

MIT License - See LICENSE file for details
