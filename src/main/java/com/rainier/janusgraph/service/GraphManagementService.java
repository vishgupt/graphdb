package com.rainier.janusgraph.service;

public interface GraphManagementService {

  public boolean createGraph(GraphDefinition graphDefinition);

  public boolean dropGraph(String graphName);

  public GraphDefinition getGraphDefinition(String graphName);

}
