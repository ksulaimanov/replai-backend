package com.replai.backend.service;

record WebhookContext(
        long botId,
        String systemPrompt,
        long chatId,
        String externalChatId,
        String incomingText,
        long telegramChatId
) {}
