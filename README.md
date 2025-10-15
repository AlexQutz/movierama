# MovieRama - Docker Setup

This document provides instructions for running the MovieRama application using Docker.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)

## Quick Start

### 1. Build and Run with Docker Compose

The easiest way to run the entire application stack:

```bash
# Build and start all services
docker-compose up --build

# Run in detached mode (background)
docker-compose up -d --build
```

This will start:
- PostgreSQL database on port 5432
- Redis cache on port 6379
- Spring Boot application on port 8080

### 2. Access the Application

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Database**: localhost:5432 (username: movierama, password: movierama123)

### 3. Stop the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This will delete all data)
docker-compose down -v
```

## Individual Container Management

### Build the Application Image

```bash
# Build the Spring Boot application image
docker build -t movierama:latest .
```

## Development Workflow

### Database Migrations

Database migrations are automatically applied when the application starts. The migration files are located in `src/main/resources/db/migration/`.

### Environment Variables

The application can be configured using environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Active Spring profile |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/movierama` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `movierama` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `movierama123` | Database password |
| `SPRING_REDIS_HOST` | `redis` | Redis host |
| `SPRING_REDIS_PORT` | `6379` | Redis port |

### Logs

```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs app
docker-compose logs postgres
docker-compose logs redis

# Follow logs in real-time
docker-compose logs -f app
```

### Health Checks

The application includes health checks for all services:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check individual components
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/redis
```

## Production Considerations

For production deployment, consider:

1. **Security**:
   - Change default passwords
   - Use secrets management
   - Enable HTTPS
   - Configure proper firewall rules

2. **Performance**:
   - Adjust JVM memory settings
   - Configure connection pools
   - Enable Redis caching
   - Use production-grade database

3. **Monitoring**:
   - Add application monitoring (e.g., Prometheus, Grafana)
   - Configure log aggregation
   - Set up alerting

4. **Scaling**:
   - Use Docker Swarm or Kubernetes
   - Configure load balancing
   - Implement horizontal scaling

## File Structure

```
movierama/
├── Dockerfile                 # Application container definition
├── docker-compose.yml        # Multi-container orchestration
├── .dockerignore            # Docker build context exclusions
├── src/main/resources/
│   ├── application.yml
└── README.md
```
