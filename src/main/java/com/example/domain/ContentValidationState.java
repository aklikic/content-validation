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
    String routingTarget) {

  public static ContentValidationState initial(ContentRequest request) {
    return new ContentValidationState(
        request.contentId(), request.payload(), request.metadata(),
        null, List.of(), null, null, ContentValidationStatus.RECEIVED, null);
  }

  public ContentValidationState withLanguage(String language) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget);
  }

  public ContentValidationState withResult(ValidationResult result) {
    var updated = new ArrayList<>(results);
    updated.add(result);
    return new ContentValidationState(contentId, payload, metadata, language, Collections.unmodifiableList(updated), aggregatedResult, reviewDecision, status, routingTarget);
  }

  public ContentValidationState withAggregatedResult(AggregatedResult aggregatedResult) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget);
  }

  public ContentValidationState withReviewDecision(ReviewDecision reviewDecision) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget);
  }

  public ContentValidationState withStatus(ContentValidationStatus status) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget);
  }

  public ContentValidationState withRoutingTarget(String routingTarget) {
    return new ContentValidationState(contentId, payload, metadata, language, results, aggregatedResult, reviewDecision, status, routingTarget);
  }
}