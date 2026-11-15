package com.example.spring_boot_project_api.service.assistant;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed, forgiving access to a tool call's string arguments. Parsing failures
 * surface as empty {@code Optional}s so a malformed value ends up as a
 * "missing/invalid argument" refusal instead of an exception.
 */
public final class AssistantArguments {

    private final Map<String, String> args;

    public AssistantArguments(Map<String, String> args) {
        this.args = args == null ? Collections.emptyMap() : args;
    }

    public Optional<String> string(String key) {
        return Optional.ofNullable(args.get(key)).filter(value -> !value.isBlank());
    }

    public Optional<Long> longValue(String key) {
        return string(key).map(value -> {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public Optional<Integer> intValue(String key) {
        return string(key).map(value -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public Optional<YearMonth> yearMonth(String key) {
        return string(key).map(value -> {
            try {
                return YearMonth.parse(value);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public Optional<LocalDate> localDate(String key) {
        return string(key).map(value -> {
            try {
                return LocalDate.parse(value);
            } catch (Exception ex) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public long requiredLong(String key) {
        return longValue(key)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing or invalid required argument '" + key + "'"));
    }

    public int requiredInt(String key) {
        return intValue(key)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing or invalid required argument '" + key + "'"));
    }

    public String requiredString(String key) {
        return string(key)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing or invalid required argument '" + key + "'"));
    }
}