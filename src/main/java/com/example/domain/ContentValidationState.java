package com.example.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ContentValidationState(
    String contentId,
    String payload,
    Map<String, String> metadata,
    String language,
    List<ValidationResult> results,
    AggregatedResult aggregatedResult,
    ReviewDecision reviewDecision,
    ContentValidationStatus status,
    String routingTarget,
    String failureReason) {

  public static ContentValidationState initial(ContentRequest request) {
    return new ContentValidationState(
        request.contentId(), request.payload(), request.metadata(),
        null, List.of(), null, null, ContentValidationStatus.RECEIVED, null, null);
  }

  public ContentValidationState withLanguage(String language) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withResult(ValidationResult result) {
    var updated = new ArrayList<>(results);
    updated.add(result);
    return new ContentValidationState(contentId, payload, metadata, language, Collections.unmodifiableList(updated), aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withAggregatedResult(AggregatedResult aggregatedResult) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withReviewDecision(ReviewDecision reviewDecision) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withStatus(ContentValidationStatus status) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withRoutingTarget(String routingTarget) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }

  public ContentValidationState withFailureReason(String failureReason) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget, failureReason);
  }
}