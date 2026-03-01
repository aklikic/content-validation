package com.example.application.guardrail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PiiGuardTest {

  private final PiiGuard guard = new PiiGuard();

  @Test
  public void shouldPassCleanText() {
    var result = guard.evaluate("This article discusses billing practices for enterprise customers.");
    assertThat(result.passed()).isTrue();
  }

  @Test
  public void shouldBlockEmail() {
    var result = guard.evaluate("Please contact john.doe@example.com for more information.");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("email");
  }

  @Test
  public void shouldBlockPhoneNumber() {
    var result = guard.evaluate("Call us at +1 (555) 123-4567 to speak with an agent.");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("phone");
  }

  @Test
  public void shouldBlockSSN() {
    var result = guard.evaluate("Customer SSN on file: 123-45-6789.");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("SSN");
  }

  @Test
  public void shouldBlockCreditCard() {
    var result = guard.evaluate("Card ending in 4111-1111-1111-1111 was charged.");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("credit card");
  }

  @Test
  public void shouldBlockFirstPiiFoundAndStopEarly() {
    var result = guard.evaluate("Email: user@test.com and SSN: 987-65-4321");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("email");
  }

  @Test
  public void shouldAllowRoleEmail() {
    var result = guard.evaluate("Contact our team at support@example.com or info@company.com for help.");
    assertThat(result.passed()).isTrue();
  }

  @Test
  public void shouldAllowTollFreePhone() {
    var result = guard.evaluate("Call us at 1-800-555-0100 for assistance.");
    assertThat(result.passed()).isTrue();
  }

  @Test
  public void shouldAllowBusinessContactInContent() {
    var result = guard.evaluate("""
        Dear Customer,
        Your account has been updated. Contact support@example.com or call 1-800-555-0100.
        Thank you, The Support Team
        """);
    assertThat(result.passed()).isTrue();
  }

  @Test
  public void shouldBlockPersonalEmailAlongsideRoleEmail() {
    var result = guard.evaluate("CC: support@example.com, john.smith@gmail.com");
    assertThat(result.passed()).isFalse();
    assertThat(result.explanation()).contains("email");
  }
}