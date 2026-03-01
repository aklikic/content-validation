package com.example.application.guardrail;

import akka.javasdk.agent.TextGuardrail;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiGuard implements TextGuardrail {

  private static final Pattern EMAIL =
      Pattern.compile("\\b([A-Za-z0-9._%+\\-]+)@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b");

  // Toll-free prefixes (800/888/877/866/855/844/833) are business numbers, not personal PII
  private static final Pattern PHONE =
      Pattern.compile("\\b(\\+\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

  private static final Pattern TOLL_FREE =
      Pattern.compile("\\b1?[-.\\s]?8(00|88|77|66|55|44|33)[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

  private static final Pattern SSN =
      Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

  private static final Pattern CREDIT_CARD =
      Pattern.compile("\\b\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b");

  // Generic role/function email addresses are business contacts, not personal PII
  private static final Set<String> ROLE_PREFIXES = Set.of(
      "support", "info", "contact", "help", "sales", "admin", "noreply", "no-reply",
      "feedback", "billing", "legal", "hr", "service", "team", "hello", "office"
  );

  @Override
  public Result evaluate(String text) {
    if (hasPersonalEmail(text)) {
      return new Result(false, "PII detected: email address found in input");
    }
    if (hasPersonalPhone(text)) {
      return new Result(false, "PII detected: phone number found in input");
    }
    if (SSN.matcher(text).find()) {
      return new Result(false, "PII detected: SSN found in input");
    }
    if (CREDIT_CARD.matcher(text).find()) {
      return new Result(false, "PII detected: credit card number found in input");
    }
    return Result.OK;
  }

  private boolean hasPersonalEmail(String text) {
    Matcher m = EMAIL.matcher(text);
    while (m.find()) {
      String localPart = m.group(1).toLowerCase();
      if (!ROLE_PREFIXES.contains(localPart)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasPersonalPhone(String text) {
    Matcher m = PHONE.matcher(text);
    while (m.find()) {
      String match = m.group();
      if (!TOLL_FREE.matcher(match).find()) {
        return true;
      }
    }
    return false;
  }
}