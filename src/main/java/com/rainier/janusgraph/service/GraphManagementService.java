package com.rainier.janusgraph.service;

import org.janusgraph.diskstorage.BackendException;

public interface GraphManagementService {

  void createGraph(GraphDefinition graphDefinition);

  void dropGraph(String graphName);

}
