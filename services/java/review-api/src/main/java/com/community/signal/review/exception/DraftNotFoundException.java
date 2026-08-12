package com.community.signal.review.exception;
import java.util.UUID;
public class DraftNotFoundException extends RuntimeException {
    public DraftNotFoundException(UUID id) { super("Draft not found: " + id); }
}
