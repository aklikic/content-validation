package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import com.example.domain.ValidationRequest;
import com.example.domain.ValidationResult;

@Component(id = "text-language-validation-agent")
@AgentRole("validator")
public class TextLanguageValidationAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Validate the text for grammar correctness and language policy compliance. Return whether it passed and a list of issues found.";

  public Effect<ValidationResult> validate(ValidationRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage("Content: " + request.content() + "\nLanguage: " + request.language())
        .responseConformsTo(ValidationResult.class)
        .thenReply();
  }
}