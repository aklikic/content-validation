package com.example.application;

import akka.Done;
import akka.javasdk.NotificationPublisher;
import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.StepName;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.workflow.Workflow;
import com.example.application.agents.LanguageDetectionAgent;
import com.example.application.agents.LocalizedNLPAgent;
import com.example.application.agents.TextLanguageValidationAgent;
import com.example.application.agents.LogoValidationAgent;
import com.example.application.agents.EnterpriseValidationAgent;
import com.example.application.agents.ValidationAggregatorAgent;
import com.example.application.agents.RoutingComplianceAgent;
import com.example.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.Duration.ofSeconds;

@Component(id = "content-validation-workflow")
public class ContentValidationWorkflow extends Workflow<ContentValidationState> {

  private static final Logger logger = LoggerFactory.getLogger(ContentValidationWorkflow.class);

  public record StatusResponse(String contentId, ContentValidationStatus status, String routingTarget) {}

  private final ComponentClient componentClient;
  private final NotificationPublisher<String> notificationPublisher;

  public ContentValidationWorkflow(ComponentClient componentClient,
                                   NotificationPublisher<String> notificationPublisher) {
    this.componentClient = componentClient;
    this.notificationPublisher = notificationPublisher;
  }

  public NotificationPublisher.NotificationStream<String> statusUpdates() {
    return notificationPublisher.stream();
  }

  @Override
  public WorkflowSettings settings() {
    return WorkflowSettings.builder()
        .defaultStepTimeout(ofSeconds(60))
        .defaultStepRecovery(maxRetries(2).failoverTo(ContentValidationWorkflow::failStep))
        .build();
  }

  // --- Command handlers ---

  public Effect<Done> start(ContentRequest request) {
    if (currentState() != null) {
      return effects().error("Workflow already started");
    }
    return effects()
        .updateState(ContentValidationState.initial(request).withStatus(ContentValidationStatus.DETECTING))
        .transitionTo(ContentValidationWorkflow::detectLanguageStep)
        .thenReply(Done.getInstance());
  }

  public ReadOnlyEffect<StatusResponse> getStatus() {
    if (currentState() == null) {
      return effects().error("Workflow not started");
    }
    return effects().reply(new StatusResponse(
        currentState().contentId(), currentState().status(), currentState().routingTarget()));
  }

  public Effect<Done> submitReview(ReviewDecision decision) {
    if (currentState() == null) {
      return effects().error("Workflow not started");
    }
    if (currentState().status() != ContentValidationStatus.AWAITING_REVIEW) {
      return effects().error("Workflow is not awaiting review, current status: " + currentState().status());
    }
    return effects()
        .updateState(currentState().withReviewDecision(decision).withStatus(ContentValidationStatus.ROUTING))
        .transitionTo(ContentValidationWorkflow::routeStep)
        .thenReply(Done.getInstance());
  }

  // --- Steps ---

  @StepName("detect-language")
  private StepEffect detectLanguageStep() {
    logger.info("Detecting language for content {}", currentState().contentId());
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(LanguageDetectionAgent::detect)
        .invoke(currentState().payload());

    notificationPublisher.publish(ContentValidationStatus.NLP.name());
    return stepEffects()
        .updateState(currentState().withLanguage(result.language()).withStatus(ContentValidationStatus.NLP))
        .thenTransitionTo(ContentValidationWorkflow::validateNLPStep);
  }

  @StepName("validate-nlp")
  private StepEffect validateNLPStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(LocalizedNLPAgent::validate)
        .invoke(new NLPRequest(currentState().payload(), currentState().language()));

    notificationPublisher.publish(ContentValidationStatus.VALIDATING_TEXT.name());
    return stepEffects()
        .updateState(currentState()
            .withResult(new ValidationResult("localized-nlp-agent", result.passed(), result.issues()))
            .withStatus(ContentValidationStatus.VALIDATING_TEXT))
        .thenTransitionTo(ContentValidationWorkflow::validateTextStep);
  }

  @StepName("validate-text")
  private StepEffect validateTextStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(TextLanguageValidationAgent::validate)
        .invoke(new ValidationRequest(currentState().payload(), currentState().language()));

    notificationPublisher.publish(ContentValidationStatus.VALIDATING_LOGO.name());
    return stepEffects()
        .updateState(currentState()
            .withResult(new ValidationResult("text-language-validation-agent", result.passed(), result.issues()))
            .withStatus(ContentValidationStatus.VALIDATING_LOGO))
        .thenTransitionTo(ContentValidationWorkflow::validateLogoStep);
  }

  @StepName("validate-logo")
  private StepEffect validateLogoStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(LogoValidationAgent::validate)
        .invoke(new LogoRequest(currentState().contentId(), currentState().payload()));

    notificationPublisher.publish(ContentValidationStatus.VALIDATING_ENTERPRISE.name());
    return stepEffects()
        .updateState(currentState()
            .withResult(new ValidationResult("logo-validation-agent", result.passed(), result.findings()))
            .withStatus(ContentValidationStatus.VALIDATING_ENTERPRISE))
        .thenTransitionTo(ContentValidationWorkflow::validateEnterpriseStep);
  }

  @StepName("validate-enterprise")
  private StepEffect validateEnterpriseStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(EnterpriseValidationAgent::validate)
        .invoke(new EnterpriseRequest(currentState().payload(), currentState().metadata()));

    notificationPublisher.publish(ContentValidationStatus.AGGREGATING.name());
    return stepEffects()
        .updateState(currentState()
            .withResult(new ValidationResult("enterprise-validation-agent", result.passed(), result.violations()))
            .withStatus(ContentValidationStatus.AGGREGATING))
        .thenTransitionTo(ContentValidationWorkflow::aggregateStep);
  }

  @StepName("aggregate")
  private StepEffect aggregateStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(ValidationAggregatorAgent::aggregate)
        .invoke(new AggregationRequest(currentState().contentId(), currentState().results()));

    logger.info("Aggregation for {}: passed={}, confidence={}", currentState().contentId(), result.overallPassed(), result.confidence());

    var newState = currentState().withAggregatedResult(result);
    if (!result.overallPassed() || result.confidence() < 0.8) {
      notificationPublisher.publish(ContentValidationStatus.AWAITING_REVIEW.name());
      return stepEffects()
          .updateState(newState.withStatus(ContentValidationStatus.AWAITING_REVIEW))
          .thenPause();
    }
    notificationPublisher.publish(ContentValidationStatus.ROUTING.name());
    return stepEffects()
        .updateState(newState.withStatus(ContentValidationStatus.ROUTING))
        .thenTransitionTo(ContentValidationWorkflow::routeStep);
  }

  @StepName("route")
  private StepEffect routeStep() {
    var result = componentClient.forAgent()
        .inSession(sessionId())
        .method(RoutingComplianceAgent::route)
        .invoke(new RoutingRequest(
            currentState().contentId(),
            currentState().aggregatedResult(),
            currentState().reviewDecision()));

    notificationPublisher.publish(ContentValidationStatus.COMPLETED.name());
    return stepEffects()
        .updateState(currentState().withRoutingTarget(result.target()).withStatus(ContentValidationStatus.COMPLETED))
        .thenEnd();
  }

  @StepName("fail")
  private StepEffect failStep() {
    logger.warn("Workflow failed for content {}", currentState().contentId());
    notificationPublisher.publish(ContentValidationStatus.FAILED.name());
    return stepEffects()
        .updateState(currentState().withStatus(ContentValidationStatus.FAILED))
        .thenEnd();
  }

  private String sessionId() {
    return commandContext().workflowId();
  }
}