package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import akka.javasdk.JsonSupport;
import com.example.domain.AggregatedResult;
import com.example.domain.AggregationRequest;

@Component(id = "validation-aggregator-agent")
@AgentRole("aggregator")
public class ValidationAggregatorAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Given a list of validation results from multiple agents, produce a consolidated report. Return overall pass/fail, a confidence score, and a brief summary of failures if any.";

  public Effect<AggregatedResult> aggregate(AggregationRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(JsonSupport.encodeToString(request))
        .responseConformsTo(AggregatedResult.class)
        .thenReply();
  }
}