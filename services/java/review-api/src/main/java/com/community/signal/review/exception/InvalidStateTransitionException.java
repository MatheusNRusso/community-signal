package com.community.signal.review.exception;
import com.community.signal.review.domain.DraftStatus;
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(DraftStatus from, DraftStatus to) {
        super("Invalid transition: " + from + " -> " + to);
    }
}
