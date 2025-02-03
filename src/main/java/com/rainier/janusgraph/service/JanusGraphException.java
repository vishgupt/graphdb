package com.rainier.janusgraph.service;

public class JanusGraphException extends RuntimeException {

  public JanusGraphException(String message, Exception e) {
    super(message, e);
  }
}
