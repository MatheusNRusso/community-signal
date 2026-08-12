#!/usr/bin/env python3
"""
E2E Smoke Test
--------------
Injects a synthetic RawEvent into `raw-events`,
waits, and verifies a message arrives in `enriched-events`.

Usage:
  python scripts/smoke_test.py

Requirements:
  - Docker stack running (make up)
  - Python deps installed (make install-all)
"""

from __future__ import annotations

import hashlib
import json
import os
import sys
import time
from datetime import datetime, timezone
from uuid import uuid4

from confluent_kafka import Producer, Consumer, KafkaException

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:19092")
RAW_TOPIC       = "raw-events"
ENRICHED_TOPIC  = "enriched-events"
TIMEOUT_S       = 30
POLL_INTERVAL   = 0.5


def make_raw_event() -> dict:
    source_id = "smoke_" + hashlib.sha256(str(uuid4()).encode()).hexdigest()[:12]
    return {
        "id":          str(uuid4()),
        "source_type": "RSS",
        "source_id":   source_id,
        "received_at": datetime.now(timezone.utc).isoformat(),
        "raw_payload": {
            "title":        "Smoke test: Python async performance improvements in 3.13",
            "content":      "The Python 3.13 release brings major improvements to async I/O performance, making it competitive with Node.js for high-throughput workloads.",
            "link":         "https://example.com/smoke-test",
            "author":       "smoke-test",
            "feed_url":     "https://example.com/feed",
            "feed_title":   "Smoke Test Feed",
            "published_at": datetime.now(timezone.utc).isoformat(),
        },
    }


def produce_event(event: dict) -> str:
    producer = Producer({"bootstrap.servers": KAFKA_BOOTSTRAP})
    payload  = json.dumps(event).encode()
    producer.produce(RAW_TOPIC, value=payload, key=event["source_id"])
    producer.flush()
    print(f"[smoke] ✅ Produced to {RAW_TOPIC}: source_id={event['source_id']}")
    return event["source_id"]


def wait_for_enriched(source_id: str) -> bool:
    consumer = Consumer({
        "bootstrap.servers": KAFKA_BOOTSTRAP,
        "group.id":          f"smoke-test-{uuid4()}",
        "auto.offset.reset": "latest",
        "enable.auto.commit": "false",
    })
    consumer.subscribe([ENRICHED_TOPIC])

    deadline = time.monotonic() + TIMEOUT_S
    print(f"[smoke] Waiting up to {TIMEOUT_S}s for event in {ENRICHED_TOPIC}...")

    try:
        while time.monotonic() < deadline:
            msg = consumer.poll(POLL_INTERVAL)
            if msg is None:
                continue
            if msg.error():
                continue
            try:
                data = json.loads(msg.value())
                if data.get("source_id") == source_id:
                    print(f"[smoke] ✅ Event found in {ENRICHED_TOPIC}")
                    print(f"[smoke]    content_hash: {data.get('content_hash', 'N/A')}")
                    print(f"[smoke]    language:     {data.get('language', 'N/A')}")
                    print(f"[smoke]    keywords:     {data.get('keywords', [])[:3]}")
                    return True
            except (json.JSONDecodeError, TypeError):
                continue
    finally:
        consumer.close()

    return False


def main() -> None:
    print("[smoke] Starting E2E smoke test...")
    print(f"[smoke] Kafka: {KAFKA_BOOTSTRAP}")

    event     = make_raw_event()
    source_id = produce_event(event)

    if wait_for_enriched(source_id):
        print("[smoke] ✅ SMOKE TEST PASSED")
        sys.exit(0)
    else:
        print(f"[smoke] ❌ SMOKE TEST FAILED — event not found in {ENRICHED_TOPIC} after {TIMEOUT_S}s")
        print("[smoke] Make sure dedup-normalizer and enricher are running.")
        sys.exit(1)


if __name__ == "__main__":
    main()
