package com.vanokal0.warehouse_backend.services;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseSpecificationService<T> {

    protected abstract Map<String, String> getFieldMapping();

    public Sort buildSort(List<String> sortFields, List<String> sortOrders) {
        if (sortFields == null || sortOrders == null || sortFields.size() != sortOrders.size()) {
            return Sort.unsorted();
        }

        Sort sort = Sort.unsorted();
        Map<String, String> fieldMapping = getFieldMapping();

        for (int i = 0; i < sortFields.size(); i++) {
            String mappedField = fieldMapping.getOrDefault(sortFields.get(i), sortFields.get(i));
            Sort single = sortOrders.get(i).equalsIgnoreCase("desc")
                    ? Sort.by(mappedField).descending()
                    : Sort.by(mappedField).ascending();
            sort = sort.and(single);
        }
        return sort;
    }

    public Specification<T> buildSpec(List<String> fields, List<String> operators, List<String> values) {
        if (fields == null || values == null || operators == null) {
            return Specification.where(null);
        }

        if (fields.size() != operators.size()) {
            return Specification.where(null);
        }

        Specification<T> spec = Specification.where(null);
        Map<String, String> fieldMapping = getFieldMapping();

        int valueIndex = 0;
        for (int i = 0; i < fields.size(); i++) {
            String field = fieldMapping.getOrDefault(fields.get(i), fields.get(i));
            String operator = operators.get(i);

            String value;

            if (operator.equals("isAnyOf")) {
                StringBuilder combined = new StringBuilder();
                for (int j = 0; j < values.size(); j++) {
                    if (valueIndex + j < values.size()) {
                        if (!combined.isEmpty()) combined.append(",");
                        combined.append(values.get(valueIndex + j));
                    }
                }
                value = combined.toString();
            } else {
                if (valueIndex >= values.size()) continue;
                value = values.get(valueIndex++);
            }

            spec = spec.and(getFilterSpec(field, operator, value));
        }

        return spec;
    }



    protected Specification<T> getFilterSpec(String fullFieldPath, String operator, String value) {
        return (root, query, cb) -> {
            Path<?> path = getPath(root, fullFieldPath);
            Class<?> type = path.getJavaType();

            if (value.equalsIgnoreCase("null")) return cb.isNull(path);

            try {
                if (type.equals(String.class)) {
                    return switch (operator) {
                        case "equals" -> cb.equal(cb.lower((Path<String>) path), value.toLowerCase());
                        case "doesNotEqual" -> cb.notEqual(cb.lower((Path<String>) path), value.toLowerCase());
                        case "contains" -> cb.like(cb.lower((Path<String>) path), "%" + value.toLowerCase() + "%");
                        case "doesNotContain" -> cb.notLike(cb.lower((Path<String>) path), "%" + value.toLowerCase() + "%");
                        case "startsWith" -> cb.like(cb.lower((Path<String>) path), value.toLowerCase() + "%");
                        case "endsWith" -> cb.like(cb.lower((Path<String>) path), "%" + value.toLowerCase());
                        case "isEmpty" -> cb.equal((Path<String>) path, "");
                        case "isNotEmpty" -> cb.notEqual((Path<String>) path, "");
                        case "isAnyOf" -> {
                            String[] parts = value.split(",");
                            CriteriaBuilder.In<String> inClause = cb.in(cb.lower((Path<String>) path));
                            for (String part : parts) {
                                inClause.value(part.trim().toLowerCase());
                            }
                            yield inClause;
                        }
                        default -> null;
                    };
                }


                if (Comparable.class.isAssignableFrom(type)) {
                    if (operator.equals("is empty")) return cb.isNull(path);
                    if (operator.equals("is not empty")) return cb.isNotNull(path);

                    @SuppressWarnings("unchecked")
                    Path<Comparable<Object>> typedPath = (Path<Comparable<Object>>) path;

                    if (operator.equals("between")) {
                        String[] parts = value.split(";");
                        if (parts.length != 2) return null;

                        Comparable<Object> start = (Comparable<Object>) castValue(parts[0], type);
                        Comparable<Object> end = (Comparable<Object>) castValue(parts[1], type);
                        return cb.between(typedPath, start, end);
                    }

                    Comparable<Object> casted = (Comparable<Object>) castValue(value, type);

                    return switch (operator) {
                        case "=", "is" -> cb.equal(typedPath, casted);
                        case "!=", "not" -> cb.notEqual(typedPath, casted);
                        case ">", "after" -> cb.greaterThan(typedPath, casted);
                        case ">=", "onOrAfter" -> cb.greaterThanOrEqualTo(typedPath, casted);
                        case "<", "before" -> cb.lessThan(typedPath, casted);
                        case "<=", "onOrBefore" -> cb.lessThanOrEqualTo(typedPath, casted);
                        default -> null;
                    };
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            return null;
        };
    }



    private Comparable<?> castValue(String value, Class<?> type) {
        try {
            if (type.equals(Integer.class)) return Integer.valueOf(value);
            if (type.equals(Long.class)) return Long.valueOf(value);
            if (type.equals(BigDecimal.class)) return new BigDecimal(value);

            if (type.equals(LocalDate.class) && value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(value);
            }

            if (type.equals(LocalDateTime.class) && value.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
                return LocalDateTime.parse(value);
            }

            if (type.equals(Instant.class)) {
                if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(value).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                }
                return Instant.parse(value);
            }

            if (value.contains("GMT") && value.contains("(")) {
                value = value.substring(0, value.indexOf("GMT") + 9).trim();
            }

            DateTimeFormatter jsFormatter = DateTimeFormatter.ofPattern(
                    "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.ENGLISH
            );

            ZonedDateTime zoned = ZonedDateTime.parse(value, jsFormatter);

            if (type.equals(LocalDate.class)) return zoned.toLocalDate();
            if (type.equals(LocalDateTime.class)) return zoned.toLocalDateTime();
            if (type.equals(Instant.class)) return zoned.toInstant();

        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse value: " + value + " to type: " + type, e);
        }

        return value;
    }

    protected Specification<T> fieldEquals(String field, Object value) {
        return (root, query, cb) -> {
            Path<?> path = getPath(root, field);
            return cb.equal(path, value);
        };
    }

    protected Path<?> getPath(Path<?> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }
}
