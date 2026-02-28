package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import akka.javasdk.JsonSupport;
import com.example.domain.EnterpriseRequest;
import com.example.domain.EnterpriseResult;

@Component(id = "enterprise-validation-agent")
@AgentRole("validator")
public class EnterpriseValidationAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Apply enterprise business rules to the content. Return whether all rules passed and list any violations.";

  public Effect<EnterpriseResult> validate(EnterpriseRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(JsonSupport.encodeToString(request))
        .responseConformsTo(EnterpriseResult.class)
        .thenReply();
  }
}