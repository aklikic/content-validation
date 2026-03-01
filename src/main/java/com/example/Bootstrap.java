package com.example;

import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import com.example.application.ContentPushConsumer;

import java.util.Set;

@Setup
public class Bootstrap implements ServiceSetup {

  @Override
  public Set<Class<?>> disabledComponents() {
    return Set.of(ContentPushConsumer.class);
  }
}