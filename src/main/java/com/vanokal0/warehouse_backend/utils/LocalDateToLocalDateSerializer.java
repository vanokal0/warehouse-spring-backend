package com.vanokal0.warehouse_backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateToLocalDateSerializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String date = p.getText();
        if (date == null || date.isEmpty()) return null;

        // Відрізаємо час, якщо прилітає з T00:00:00
        if (date.contains("T")) {
            date = date.substring(0, date.indexOf("T"));
        }

        return LocalDate.parse(date, formatter);
    }
}
