package com.vanokal0.warehouse_backend.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateDeserializersTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonParser parserFor(String json) throws IOException {
        JsonParser p = MAPPER.createParser(json);
        p.nextToken();
        return p;
    }

    @Test
    void localDateTimeToInstant_interpretsValueInSystemZone() throws IOException {
        Instant result = new LocalDateTimeToInstantDeserializer()
                .deserialize(parserFor("\"2025-03-04T10:15:30\""), null);

        assertThat(result).isEqualTo(
                LocalDateTime.of(2025, 3, 4, 10, 15, 30).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void localDateTimeToInstant_rejectsValuesWithOffset() {
        assertThatThrownBy(() -> new LocalDateTimeToInstantDeserializer()
                .deserialize(parserFor("\"2025-03-04T10:15:30Z\""), null))
                .isInstanceOf(InvalidFormatException.class)
                .satisfies(e -> assertThat(((InvalidFormatException) e).getValue()).isEqualTo("2025-03-04T10:15:30Z"));
    }

    @Test
    void localDateTimeToInstant_rejectsGarbage() {
        assertThatThrownBy(() -> new LocalDateTimeToInstantDeserializer().deserialize(parserFor("\"tomorrow\""), null))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void localDate_parsesIsoDate() throws IOException {
        assertThat(new LocalDateToLocalDateSerializer().deserialize(parserFor("\"2025-03-04\""), null))
                .isEqualTo(LocalDate.of(2025, 3, 4));
    }

    @Test
    void localDate_stripsTimePart() throws IOException {
        assertThat(new LocalDateToLocalDateSerializer().deserialize(parserFor("\"2025-03-04T00:00:00.000Z\""), null))
                .isEqualTo(LocalDate.of(2025, 3, 4));
    }

    @Test
    void localDate_returnsNullForEmptyString() throws IOException {
        assertThat(new LocalDateToLocalDateSerializer().deserialize(parserFor("\"\""), null)).isNull();
    }

    @Test
    void localDate_rejectsInvalidDate() {
        assertThatThrownBy(() -> new LocalDateToLocalDateSerializer().deserialize(parserFor("\"04/03/2025\""), null))
                .isInstanceOf(DateTimeParseException.class);
    }
}
