# Community Signal Platform - Architecture Overview

## Overview

Community Signal Platform is a distributed platform designed to collect, process, enrich, rank, generate and publish high-signal social media content.

The platform follows an event-driven architecture using Kafka as the backbone for communication between services.

Primary goals:

* Discover relevant community discussions
* Remove duplicated content
* Generate embeddings
* Cluster related conversations
* Generate content drafts using LLMs
* Apply guardrails and moderation
* Enable human review
* Publish approved content

---

## High-Level Architecture

```text
Source Adapters
        ↓
Dedup Normalizer
        ↓
Embedding Service
        ↓
Clustering Summarizer
        ↓
Draft Generator
        ↓
Guardrails
        ↓
Review API
        ↓
approved-drafts
        ↓
Publisher
```

---

## Java Services

### review-api

Responsibilities:

* Human review workflow
* Draft approval and rejection
* Review metrics
* Publishing approved events to Kafka

Technology:

* Java 21
* Spring Boot
* PostgreSQL
* Kafka

---

### ranking-engine

Responsibilities:

* Signal ranking
* Content prioritization
* Engagement scoring support

Technology:

* Java 21
* Spring Boot

---

### engagement-scorer

Responsibilities:

* Calculate engagement scores
* Produce ranking features
* Support recommendation pipelines

Technology:

* Java 21
* Spring Boot

---

## Python Services

### source-adapters

Responsibilities:

* Collect content from external sources
* Normalize incoming events

---

### dedup-normalizer

Responsibilities:

* Detect duplicates
* Canonicalize content

---

### embedding-service

Responsibilities:

* Generate vector embeddings
* Support semantic similarity

Potential technologies:

* Sentence Transformers
* OpenAI Embeddings
* pgvector

---

### clustering-summarizer

Responsibilities:

* Group similar discussions
* Generate cluster summaries

---

### draft-generator

Responsibilities:

* Generate publication drafts
* Produce channel-specific content

---

### guardrails

Responsibilities:

* Safety checks
* Policy validation
* Brand compliance

---

### publisher

Responsibilities:

* Publish approved content
* Integrate with external channels

---

## Data Stores

### PostgreSQL

Used for:

* Draft persistence
* Review workflow
* Audit data
* Metadata storage

---

### Redis

Used for:

* Caching
* Temporary state
* Rate limiting

---

### Kafka

Used for:

* Inter-service communication
* Event streaming
* Decoupling services

---

## Current Kafka Topics

### approved-drafts

Produced by:

* review-api

Consumed by:

* publisher

Purpose:

* Deliver approved content for publication

---

## Review Workflow

```text
Draft Generated
        ↓
PENDING
        ↓
IN_REVIEW
        ↓
APPROVED ─────► Kafka Topic
        ↓
REJECTED
```

---

## CI/CD

GitHub Actions validates:

* Java services
* Python services
* Lint checks
* Test reports

Current status:

* CI Green
* Node 24 migration prepared

---

## Future Roadmap

### Short Term

* Review metrics dashboard
* Reviewer audit history
* Publishing observability

### Medium Term

* Multi-channel publishing
* Vector search
* Recommendation engine

### Long Term

* Real-time signal discovery
* Agent-based content generation
* Fully automated publishing pipelines


