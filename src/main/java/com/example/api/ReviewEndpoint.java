package com.example.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.example.application.ContentStatusView;
import com.example.application.ContentValidationWorkflow;
import com.example.domain.ReviewDecision;

@HttpEndpoint("/reviews")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
public class ReviewEndpoint {

  private final ComponentClient componentClient;

  public ReviewEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/{contentId}/decision")
  public HttpResponse submitDecision(String contentId, ReviewDecision decision) {
    componentClient.forWorkflow(contentId)
        .method(ContentValidationWorkflow::submitReview)
        .invoke(decision);
    return HttpResponses.ok();
  }

  @Get("/status/stream")
  public HttpResponse streamAll() {
    var source = componentClient
        .forView()
        .stream(ContentStatusView::streamAll)
        .entriesSource();
    return HttpResponses.serverSentEventsForView(source);
  }

  @Get("/pending/stream")
  public HttpResponse streamPendingReviews() {
    var source = componentClient
        .forView()
        .stream(ContentStatusView::streamPendingReviews)
        .entriesSource();
    return HttpResponses.serverSentEventsForView(source);
  }
}