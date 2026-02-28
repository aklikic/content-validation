package com.example.application;

import akka.javasdk.JsonSupport;
import akka.javasdk.testkit.EventingTestKit;
import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import akka.javasdk.testkit.TestModelProvider;
import com.example.application.agents.*;
import com.example.domain.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ContentValidationWorkflowIntegrationTest extends TestKitSupport {

  private final TestModelProvider languageModel = new TestModelProvider();
  private final TestModelProvider nlpModel = new TestModelProvider();
  private final TestModelProvider textModel = new TestModelProvider();
  private final TestModelProvider logoModel = new TestModelProvider();
  private final TestModelProvider enterpriseModel = new TestModelProvider();
  private final TestModelProvider aggregatorModel = new TestModelProvider();
  private final TestModelProvider routerModel = new TestModelProvider();

  private EventingTestKit.OutgoingMessages contentPushTopic;

  @BeforeAll
  public void beforeAll() {
    super.beforeAll();
    contentPushTopic = testKit.getTopicOutgoingMessages("content-push");
  }

  @Override
  protected TestKit.Settings testKitSettings() {
    return TestKit.Settings.DEFAULT
        .withAdditionalConfig("akka.javasdk.agent.openai.api-key = n/a")
        .withTopicOutgoingMessages("content-push")
        .withModelProvider(LanguageDetectionAgent.class, languageModel)
        .withModelProvider(LocalizedNLPAgent.class, nlpModel)
        .withModelProvider(TextLanguageValidationAgent.class, textModel)
        .withModelProvider(LogoValidationAgent.class, logoModel)
        .withModelProvider(EnterpriseValidationAgent.class, enterpriseModel)
        .withModelProvider(ValidationAggregatorAgent.class, aggregatorModel)
        .withModelProvider(RoutingComplianceAgent.class, routerModel);
  }

  private void setupValidationMocks() {
    languageModel.fixedResponse(JsonSupport.encodeToString(new DetectionResult("en", 0.99)));
    nlpModel.fixedResponse(JsonSupport.encodeToString(new NLPResult("billing", true, List.of())));
    textModel.fixedResponse(JsonSupport.encodeToString(new ValidationResult("unused", true, List.of())));
    logoModel.fixedResponse(JsonSupport.encodeToString(new LogoResult(true, List.of())));
    enterpriseModel.fixedResponse(JsonSupport.encodeToString(new EnterpriseResult(true, List.of())));
    routerModel.fixedResponse(JsonSupport.encodeToString(new RoutingDecision("channel-a", true, "Compliant")));
  }

  private ContentStatusView.StatusEntry awaitViewEntry(String contentId, String expectedStatus) {
    var ref = new ContentStatusView.StatusEntry[1];
    Awaitility.await()
        .ignoreExceptions()
        .atMost(10, SECONDS)
        .untilAsserted(() -> {
          var entry = componentClient.forView()
              .method(ContentStatusView::getAll)
              .invoke()
              .entries()
              .stream()
              .filter(e -> e.contentId().equals(contentId) && e.status().equals(expectedStatus))
              .findFirst();
          assertThat(entry).isPresent();
          ref[0] = entry.get();
        });
    return ref[0];
  }

  @Test
  public void shouldCompleteWorkflowWhenAllValidationsPass() {
    setupValidationMocks();
    aggregatorModel.fixedResponse(JsonSupport.encodeToString(
        new AggregatedResult(true, 0.95, "All checks passed")));

    var contentId = UUID.randomUUID().toString();
    var request = new ContentRequest(contentId, "Hello world content", Map.of("type", "article"));

    componentClient.forWorkflow(contentId)
        .method(ContentValidationWorkflow::start)
        .invoke(request);

    Awaitility.await()
        .ignoreExceptions()
        .atMost(10, SECONDS)
        .untilAsserted(() -> {
          var status = componentClient.forWorkflow(contentId)
              .method(ContentValidationWorkflow::getStatus)
              .invoke();
          assertThat(status.status()).isEqualTo(ContentValidationStatus.COMPLETED);
          assertThat(status.routingTarget()).isEqualTo("channel-a");
          assertThat(status.contentId()).isEqualTo(contentId);
        });

    var entry = awaitViewEntry(contentId, "COMPLETED");
    assertThat(entry.language()).isEqualTo("en");
    assertThat(entry.routingTarget()).isEqualTo("channel-a");
    assertThat(entry.aggregatedResult().confidence()).isEqualTo(0.95);
    assertThat(entry.aggregatedResult().summary()).isEqualTo("All checks passed");
    assertThat(entry.results()).hasSize(4);

    var pushed = contentPushTopic.expectOneTyped(PushRequest.class, ofSeconds(5));
    assertThat(pushed.getPayload().contentId()).isEqualTo(contentId);
    assertThat(pushed.getPayload().target()).isEqualTo("channel-a");
    assertThat(pushed.getPayload().payload()).isEqualTo("Hello world content");
  }

  @Test
  public void shouldPauseForReviewWhenAggregationConfidenceIsLow() {
    setupValidationMocks();
    aggregatorModel.fixedResponse(JsonSupport.encodeToString(
        new AggregatedResult(true, 0.7, "Low confidence, needs human review")));

    var contentId = UUID.randomUUID().toString();
    var request = new ContentRequest(contentId, "Uncertain content", Map.of("type", "article"));

    componentClient.forWorkflow(contentId)
        .method(ContentValidationWorkflow::start)
        .invoke(request);

    Awaitility.await()
        .ignoreExceptions()
        .atMost(10, SECONDS)
        .untilAsserted(() -> {
          var status = componentClient.forWorkflow(contentId)
              .method(ContentValidationWorkflow::getStatus)
              .invoke();
          assertThat(status.status()).isEqualTo(ContentValidationStatus.AWAITING_REVIEW);
        });

    var pendingEntry = awaitViewEntry(contentId, "AWAITING_REVIEW");
    assertThat(pendingEntry.aggregatedResult().confidence()).isEqualTo(0.7);
    assertThat(pendingEntry.aggregatedResult().summary()).isEqualTo("Low confidence, needs human review");
    assertThat(pendingEntry.reviewDecision()).isNull();

    var review = new ReviewDecision("approve", "reviewer-1", "Looks good after manual check");
    componentClient.forWorkflow(contentId)
        .method(ContentValidationWorkflow::submitReview)
        .invoke(review);

    Awaitility.await()
        .ignoreExceptions()
        .atMost(10, SECONDS)
        .untilAsserted(() -> {
          var status = componentClient.forWorkflow(contentId)
              .method(ContentValidationWorkflow::getStatus)
              .invoke();
          assertThat(status.status()).isEqualTo(ContentValidationStatus.COMPLETED);
          assertThat(status.routingTarget()).isEqualTo("channel-a");
        });

    var completedEntry = awaitViewEntry(contentId, "COMPLETED");
    assertThat(completedEntry.routingTarget()).isEqualTo("channel-a");
    assertThat(completedEntry.reviewDecision().decision()).isEqualTo("approve");
    assertThat(completedEntry.reviewDecision().reviewer()).isEqualTo("reviewer-1");

    var pushed = contentPushTopic.expectOneTyped(PushRequest.class, ofSeconds(5));
    assertThat(pushed.getPayload().contentId()).isEqualTo(contentId);
    assertThat(pushed.getPayload().target()).isEqualTo("channel-a");
    assertThat(pushed.getPayload().payload()).isEqualTo("Uncertain content");
  }
}