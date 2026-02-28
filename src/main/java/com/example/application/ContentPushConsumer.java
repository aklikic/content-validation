package com.example.application;

import akka.javasdk.Metadata;
import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.DeleteHandler;
import akka.javasdk.annotations.Produce;
import akka.javasdk.consumer.Consumer;
import com.example.domain.ContentValidationState;
import com.example.domain.ContentValidationStatus;
import com.example.domain.PushRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(id = "content-push-consumer")
@Consume.FromWorkflow(ContentValidationWorkflow.class)
@Produce.ToTopic("content-push")
public class ContentPushConsumer extends Consumer {

  private static final Logger logger = LoggerFactory.getLogger(ContentPushConsumer.class);

  public Effect onUpdate(ContentValidationState state) {
    if (state.status() == ContentValidationStatus.COMPLETED && state.routingTarget() != null) {
      logger.info("Publishing content {} to topic for target {}", state.contentId(), state.routingTarget());
      var pushRequest = new PushRequest(state.contentId(), state.routingTarget(), state.payload());
      var metadata = Metadata.EMPTY.add("ce-subject", state.contentId());
      return effects().produce(pushRequest, metadata);
    }
    return effects().ignore();
  }

  @DeleteHandler
  public Effect onDelete() {
    return effects().ignore();
  }
}