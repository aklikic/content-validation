package com.example.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.example.application.ContentValidationWorkflow;
import com.example.domain.ContentRequest;

@HttpEndpoint("/content")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
public class ContentEndpoint {

  public record SubmitResponse(String contentId, String status) {}

  public record StatusResponse(String contentId, String status, String routingTarget) {}

  private final ComponentClient componentClient;

  public ContentEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post
  public HttpResponse submit(ContentRequest request) {
    componentClient.forWorkflow(request.contentId())
        .method(ContentValidationWorkflow::start)
        .invoke(request);
    return HttpResponses.created(
        new SubmitResponse(request.contentId(), "RECEIVED"),
        "/content/" + request.contentId() + "/status");
  }

  @Get("/{contentId}/status")
  public StatusResponse getStatus(String contentId) {
    var status = componentClient.forWorkflow(contentId)
        .method(ContentValidationWorkflow::getStatus)
        .invoke();
    return new StatusResponse(status.contentId(), status.status().name(), status.routingTarget());
  }

  @Get("/{contentId}/stream")
  public HttpResponse streamStatus(String contentId) {
    return HttpResponses.serverSentEvents(
        componentClient.forWorkflow(contentId)
            .notificationStream(ContentValidationWorkflow::statusUpdates)
            .source());
  }
}