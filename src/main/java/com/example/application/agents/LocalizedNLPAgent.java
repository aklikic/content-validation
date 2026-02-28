package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import com.example.domain.NLPRequest;
import com.example.domain.NLPResult;

@Component(id = "localized-nlp-agent")
@AgentRole("validator")
public class LocalizedNLPAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Classify the call reason from the content and validate it meets localization requirements for the detected language. Return the call reason category and whether it passed.";

  public Effect<NLPResult> validate(NLPRequest request) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage("Content: " + request.content() + "\nLanguage: " + request.language())
        .responseConformsTo(NLPResult.class)
        .thenReply();
  }
}