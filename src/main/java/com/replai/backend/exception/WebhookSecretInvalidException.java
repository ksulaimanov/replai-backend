package com.replai.backend.exception;

public class WebhookSecretInvalidException extends RuntimeException {
    public WebhookSecretInvalidException() {
        super("Invalid webhook secret token");
    }
}
