package com.rainier.janusgraph.service;

import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

public class JanusGraphService implements GraphService{

  private String graphName;
  private JanusGraph janusGraph;

  public JanusGraphService(String graphName) {
    this.graphName = graphName;
    this.janusGraph = JanusGraphFactory.open(graphName);
  }
  @Override
  public Object query(String query) {
    return null;
  }
}
