package com.github.msvidal.transactionauthorizer.infra.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeEpochDeserializer extends JsonDeserializer<OffsetDateTime> {

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            long epoch = Long.parseLong(text);
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
        } catch (NumberFormatException ex) {
            try {
                return OffsetDateTime.parse(text);
            } catch (Exception e) {
                throw ctxt.weirdStringException(text, OffsetDateTime.class, "Não foi possível desserializar created_at para OffsetDateTime");
            }
        }
    }
}
