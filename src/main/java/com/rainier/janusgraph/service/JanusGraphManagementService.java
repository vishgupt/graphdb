package com.rainier.janusgraph.service;

import com.rainier.janusgraph.service.GraphSchema.EdgeLabel;
import com.rainier.janusgraph.service.GraphSchema.Index;
import com.rainier.janusgraph.service.GraphSchema.PropertyKey;
import com.rainier.janusgraph.service.GraphSchema.VertexLabel;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.core.schema.JanusGraphManagement;

public class JanusGraphManagementService implements GraphManagementService {

  JanusGraphFactory janusGraphFactory;

  public JanusGraphManagementService(JanusGraphFactory janusGraphFactory) {
    this.janusGraphFactory = janusGraphFactory;
  }


  @Override
  public boolean createGraph(GraphDefinition graphDefinition) {

    // Create a new configuration object
    Configuration config = loadConfigurationFromFile("src/main/resources/janusgraph-cassandra.properties");
    config.setProperty("storage.cassandra.keyspace", graphDefinition.graphName());

    JanusGraph janusGraph = janusGraphFactory.open(config);
    JanusGraphManagement graphManagement = janusGraph.openManagement();

    GraphSchema graphSchema = graphDefinition.schema();

    for (VertexLabel vertexLabel : graphSchema.vertexLabels()) {
      if (!graphManagement.containsVertexLabel(vertexLabel.name())) {
        org.janusgraph.core.VertexLabel label = graphManagement
            .makeVertexLabel(vertexLabel.name())
            .make();
      }
    }

    for (EdgeLabel edgeLabel : graphSchema.edgeLabels()) {
      if (!graphManagement.containsEdgeLabel(edgeLabel.name())) {
        graphManagement.makeEdgeLabel(edgeLabel.name())
            .multiplicity(Multiplicity.valueOf(edgeLabel.multiplicity()))
            .make();
      }
    }

    for (PropertyKey property : graphSchema.propertyKeys()) {
      if (!graphManagement.containsPropertyKey(property.name())) {
        graphManagement.makePropertyKey(property.name())
            .dataType(getDataType(property.dataType()))
            .cardinality(Cardinality.valueOf(property.cardinality()))
            .make();
      }
    }

    for (Index index : graphSchema.indexes()) {
      if (graphManagement.containsGraphIndex(index.name())) {
        // do nothing
      }
    }

    graphManagement.commit();
    return false;
  }

  private static Class getDataType(String dataTypeString) {
    return switch (dataTypeString) {
      case "string" -> String.class;
      case "int" -> Integer.class;
      case "long" -> Long.class;
      case "float" -> Float.class;
      case "double" -> Double.class;
      case "boolean" -> Boolean.class;
      default -> throw new IllegalArgumentException("Invalid data type: " + dataTypeString);
    };
  }

  /**
   * Loads a configuration from a properties file.
   *
   * @param filePath Path to the properties file.
   * @return An Apache Commons Configuration object.
   */
  private static Configuration loadConfigurationFromFile(String filePath) {
    Properties properties = new Properties();
    BaseConfiguration config = new BaseConfiguration();

    try (FileInputStream fileInput = new FileInputStream(filePath)) {
      // Load properties file
      properties.load(fileInput);
      // Populate BaseConfiguration with properties
      properties.forEach((key, value) -> config.setProperty((String) key, value));
      return config;
    } catch (IOException e) {
      System.err.println("Error reading properties file: " + e.getMessage());
      return null;
    }
  }

  @Override
  public boolean dropGraph(String graphName) {
    return false;
  }

  @Override
  public GraphDefinition getGraphDefinition(String graphName) {
    return null;
  }
}
