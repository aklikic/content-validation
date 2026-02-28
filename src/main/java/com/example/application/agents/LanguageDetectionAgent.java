package com.example.application.agents;

import akka.javasdk.agent.Agent;
import akka.javasdk.annotations.AgentRole;
import akka.javasdk.annotations.Component;
import com.example.domain.DetectionResult;

@Component(id = "language-detection-agent")
@AgentRole("validator")
public class LanguageDetectionAgent extends Agent {

  private static final String SYSTEM_MESSAGE =
      "Detect the language of the provided text. Return the ISO 639-1 language code and a confidence score between 0 and 1.";

  public Effect<DetectionResult> detect(String content) {
    return effects()
        .systemMessage(SYSTEM_MESSAGE)
        .userMessage(content)
        .responseConformsTo(DetectionResult.class)
        .thenReply();
  }
}