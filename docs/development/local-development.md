# Local Development Workflow

## Prerequisites

- Docker
- docker-compose
- Java 21
- Maven
- Python 3.12
- Node.js
- npm
- GitHub CLI

## Start infrastructure

    docker-compose up -d postgres redis zookeeper kafka schema-registry kafka-init

## Validate infrastructure

    docker-compose ps

    PGPASSWORD=signal_pass psql \
      -h localhost \
      -p 19432 \
      -U signal_user \
      -d community_signal \
      -c "select 1;"

    docker-compose exec kafka kafka-topics \
      --bootstrap-server localhost:9092 \
      --list

Expected Kafka topic:

    approved-drafts

## Start Review API

    cd services/java/review-api

    DATABASE_USER=signal_user \
    DATABASE_PASSWORD=signal_pass \
    mvn spring-boot:run

## Start Frontend

    cd frontend
    npm install
    npm start

Open:

    http://localhost:4200/review

## Review flow validation

1. Open the review dashboard.
2. Select a draft.
3. Click `Start Review`.
4. Add a review note.
5. Click `Approve` or `Reject`.

Expected backend logs:

    draft.review.started
    draft.approved
    draft.published

## Run Java tests

    cd services/java
    mvn clean test

## GitHub Actions

    gh run list --limit 5
    gh run watch
    gh run view --log
