package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import com.example.domain.LogoRequest;
import com.example.domain.LogoResult;

@Component(id = "logo-validation-agent")
@AgentRole("validator")
public class LogoValidationAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Check whether required logos are present and compliant with brand guidelines. Return pass/fail and any findings.";

  public Effect<LogoResult> validate(LogoRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage("Content ID: " + request.contentId() + "\nContent URL: " + request.contentUrl())
        .responseConformsTo(LogoResult.class)
        .thenReply();
  }
}