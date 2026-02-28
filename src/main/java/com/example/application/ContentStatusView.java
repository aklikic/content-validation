package com.example.application;

import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import com.example.domain.*;

import java.util.List;

@Component(id = "content-status-view")
public class ContentStatusView extends View {

  public record StatusEntry(
      String contentId,
      String payload,
      String language,
      List<ValidationResult> results,
      AggregatedResult aggregatedResult,
      ReviewDecision reviewDecision,
      String status,
      String routingTarget) {}

  public record StatusEntries(List<StatusEntry> entries) {}

  @Consume.FromWorkflow(ContentValidationWorkflow.class)
  public static class ContentStatusUpdater extends TableUpdater<StatusEntry> {

    public Effect<StatusEntry> onUpdate(ContentValidationState state) {
      return effects().updateRow(new StatusEntry(
          state.contentId(),
          state.payload(),
          state.language() != null ? state.language() : "",
          state.results(),
          state.aggregatedResult(),
          state.reviewDecision(),
          state.status().name(),
          state.routingTarget() != null ? state.routingTarget() : ""));
    }
  }

  @Query("SELECT * AS entries FROM content_status")
  public QueryEffect<StatusEntries> getAll() {
    return queryResult();
  }

  @Query(value = "SELECT * FROM content_status", streamUpdates = true)
  public QueryStreamEffect<StatusEntry> streamAll() {
    return queryStreamResult();
  }

  @Query(value = "SELECT * FROM content_status WHERE status = 'AWAITING_REVIEW'", streamUpdates = true)
  public QueryStreamEffect<StatusEntry> streamPendingReviews() {
    return queryStreamResult();
  }
}