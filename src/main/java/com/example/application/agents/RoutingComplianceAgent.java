package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import akka.javasdk.JsonSupport;
import com.example.domain.RoutingDecision;
import com.example.domain.RoutingRequest;

@Component(id = "routing-compliance-agent")
@AgentRole("router")
public class RoutingComplianceAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Determine the routing destination for the content based on its validation outcome and apply final compliance checks. Return the target platform and compliance status.";

  public Effect<RoutingDecision> route(RoutingRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(JsonSupport.encodeToString(request))
        .responseConformsTo(RoutingDecision.class)
        .thenReply();
  }
}