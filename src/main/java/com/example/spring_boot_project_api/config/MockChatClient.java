package com.example.spring_boot_project_api.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.service.assistant.dto.AiChatMessage;
import com.example.spring_boot_project_api.service.assistant.dto.AiPrediction;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;

/**
 * Deterministic mock LLM used during Phase A. It does not call any external
 * service; instead it routes a free-text message to a tool using keyword
 * matching over the descriptive tool metadata. All real authorization and data
 * access still happens in the tool executor, so the mock only replaces the
 * "understanding" part of the pipeline (to be swapped for Ollama in Phase B).
 *
 * <p>Conversation contract:
 * <ul>
 *   <li>a turn whose last message has role {@code tool} is answered with that
 *       tool result verbatim (final reply);</li>
 *   <li>otherwise the last message is treated as a user turn. When it matches a
 *       tool it returns a tool call; otherwise it echoes the message.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock")
public class MockChatClient implements AiChatClient {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_TENANT = "TENANT";

    private static final Pattern HOUSE_ID = Pattern.compile(
            "house\\s*(?:id)?\\s*[#:=]?\\s*(\\d+)");
    private static final Pattern INVOICE_ID = Pattern.compile(
            "invoice\\s*(?:id)?\\s*[#:=]?\\s*(\\d+)");
    private static final Pattern YEAR_MONTH = Pattern.compile(
            "\\b((?:19|20)\\d{2})-(\\d{1,2})\\b");
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern MONTH = Pattern.compile("\\bmonth\\s*[:= ]?\\s*(\\d{1,2})\\b");
    private static final Pattern FROM = Pattern.compile("from\\s*[:= ]?\\s*(\\d{4}-\\d{2})");
    private static final Pattern TO = Pattern.compile("to\\s*[:= ]?\\s*(\\d{4}-\\d{2})");

    @Override
    public AiPrediction chat(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes) {
        AiChatMessage last = messages.get(messages.size() - 1);

        if ("tool".equals(last.role())) {
            return AiPrediction.reply(last.content() == null ? "" : last.content());
        }

        String text = last.content() == null ? "" : last.content().trim().toLowerCase(Locale.ROOT);
        Set<String> roles = new HashSet<>(roleCodes == null ? List.of() : roleCodes);
        Map<String, String> args = new LinkedHashMap<>();

        String toolName = matchTool(text, roles, args);
        if (toolName != null) {
            List<AiToolCall> calls = new ArrayList<>();
            calls.add(new AiToolCall(toolName, new LinkedHashMap<>(args)));
            return new AiPrediction(null, calls);
        }

        return AiPrediction.reply("Mock reply: I received \"" + last.content().trim() + "\"");
    }

    @Override
    public AiPrediction chatStream(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes, Consumer<String> textDelta) {
        AiPrediction result = chat(messages, tools, roleCodes);
        if (result.content() != null && !result.content().isBlank()) {
            // Split reply into ~3-word chunks to simulate token streaming.
            // No sleep — keeps tests fast and deterministic.
            String[] words = result.content().split("(?<=\\s)|(?=\\s)");
            StringBuilder chunk = new StringBuilder();
            int wordCount = 0;
            for (String token : words) {
                chunk.append(token);
                if (!token.isBlank()) {
                    wordCount++;
                }
                if (wordCount >= 3) {
                    textDelta.accept(chunk.toString());
                    chunk.setLength(0);
                    wordCount = 0;
                }
            }
            if (!chunk.isEmpty()) {
                textDelta.accept(chunk.toString());
            }
        }
        return result;
    }

    private String matchTool(String text, Set<String> roles, Map<String, String> args) {
        boolean tenant = roles.contains(ROLE_TENANT);
        boolean staff = roles.contains(ROLE_OWNER) || roles.contains(ROLE_MANAGER);

        if (containsAny(text, "overdue")) {
            captureOptionalHouse(text, args);
            captureYearMonth(text, args);
            return "get_overdue_invoices";
        }
        if (containsAny(text, "vacant")) {
            captureOptionalHouse(text, args);
            return "get_vacant_rooms";
        }
        if (containsAny(text, "revenue")) {
            captureYearMonth(text, args);
            return staff ? "get_monthly_report" : "tenant_balance";
        }
        if (containsAny(text, "monthly") || containsAny(text, "report")) {
            captureYearMonth(text, args);
            return tenant ? "tenant_payment_history" : "get_monthly_report";
        }
        if (containsAny(text, "dashboard")) {
            return "get_dashboard_summary";
        }
        if (containsAny(text, "summary")) {
            captureOptionalHouse(text, args);
            return "get_house_summary";
        }
        if (containsAny(text, "electricity") || containsAny(text, "water")
                || containsAny(text, "utility")) {
            captureYearMonth(text, args);
            return tenant ? "tenant_utility_trend" : "get_monthly_report";
        }
        if (containsAny(text, "balance") || containsAny(text, "owe") || containsAny(text, "due")) {
            return tenant ? "tenant_balance" : "get_dashboard_summary";
        }
        if (containsAny(text, "payment")) {
            captureYearMonth(text, args);
            return tenant ? "tenant_payment_history" : "get_overdue_invoices";
        }
        if (containsAny(text, "profile")) {
            return "tenant_profile";
        }
        if (containsAny(text, "houses")) {
            return "get_my_houses";
        }
        if (containsAny(text, "rooms")) {
            captureOptionalHouse(text, args);
            return "get_house_rooms";
        }
        if (containsAny(text, "invoice")) {
            captureOptionalHouse(text, args);
            captureYearMonth(text, args);

            Long invoiceId = captureNumberAfter(text, INVOICE_ID);
            if (invoiceId != null && staff) {
                args.put("invoiceId", String.valueOf(invoiceId));
                return "get_invoice";
            }
            return tenant ? "tenant_invoices" : "get_invoices";
        }
        if (tenant && containsAny(text, "history")) {
            return "tenant_payment_history";
        }
        if (containsAny(text, "house")) {
            captureOptionalHouse(text, args);
            return "get_house_summary";
        }
        return null;
    }

    private void captureOptionalHouse(String text, Map<String, String> args) {
        Long value = captureNumberAfter(text, HOUSE_ID);
        if (value != null) {
            args.put("houseId", String.valueOf(value));
        }
    }

    private void captureYearMonth(String text, Map<String, String> args) {
        Matcher ym = YEAR_MONTH.matcher(text);
        if (ym.find()) {
            args.put("year", ym.group(1));
            args.put("month", ym.group(2));
            return;
        }
        Matcher year = YEAR.matcher(text);
        if (year.find()) {
            args.put("year", year.group());
        }
        Matcher month = MONTH.matcher(text);
        if (month.find()) {
            args.put("month", month.group(1));
        }
    }

    private static Long captureNumberAfter(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}