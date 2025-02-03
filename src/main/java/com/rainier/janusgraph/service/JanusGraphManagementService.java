package com.rainier.janusgraph.service;

import com.rainier.janusgraph.service.GraphSchema.Connection;
import com.rainier.janusgraph.service.GraphSchema.EdgeIndex;
import com.rainier.janusgraph.service.GraphSchema.EdgeLabel;
import com.rainier.janusgraph.service.GraphSchema.PropertyKey;
import com.rainier.janusgraph.service.GraphSchema.VertexIndex;
import com.rainier.janusgraph.service.GraphSchema.VertexLabel;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.core.schema.EdgeLabelMaker;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.RelationTypeIndex;
import org.janusgraph.diskstorage.BackendException;

public class JanusGraphManagementService implements GraphManagementService {

  JanusGraphFactory janusGraphFactory;
  KeySpaceBuilder keySpaceBuilder;

  public JanusGraphManagementService(JanusGraphFactory janusGraphFactory, KeySpaceBuilder keySpaceBuilder) {
    this.janusGraphFactory = janusGraphFactory;
    this.keySpaceBuilder = keySpaceBuilder;
  }


  @Override
  public void createGraph(GraphDefinition graphDefinition) {
    keySpaceBuilder.build(graphDefinition.graphName());
    // Create a new configuration object
    JanusGraph janusGraph = getJanusGraphManagement(graphDefinition.graphName());
    JanusGraphManagement graphManagement = janusGraph.openManagement();
    GraphSchema graphSchema = graphDefinition.schema();

    for (VertexLabel vertexLabel : graphSchema.vertexLabels()) {
      if (!graphManagement.containsVertexLabel(vertexLabel.name())) {
        graphManagement.makeVertexLabel(vertexLabel.name()).make();
      }
    }

    for (EdgeLabel edgeLabel : graphSchema.edgeLabels()) {
      if (!graphManagement.containsEdgeLabel(edgeLabel.name())) {
        EdgeLabelMaker edgeLabelMaker = graphManagement.makeEdgeLabel(edgeLabel.name())
            .multiplicity(Multiplicity.valueOf(edgeLabel.multiplicity()));
        if (edgeLabel.directed()) {
          edgeLabelMaker.directed();
        } else {
          edgeLabelMaker.unidirected();
        }
        edgeLabelMaker.make();

        org.janusgraph.core.EdgeLabel label = graphManagement.getEdgeLabel(edgeLabel.name());
        for (Connection connection : edgeLabel.connections()) {
          org.janusgraph.core.VertexLabel fromVertex = graphManagement.getVertexLabel(connection.from());
          org.janusgraph.core.VertexLabel toVertex = graphManagement.getVertexLabel(connection.from());
          graphManagement.addConnection(label, fromVertex, toVertex);
        }
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

    for (VertexLabel vertexLabel : graphSchema.vertexLabels()) {
      if (graphManagement.containsVertexLabel(vertexLabel.name())) {
        org.janusgraph.core.VertexLabel label = graphManagement.getVertexLabel(vertexLabel.name());
        org.janusgraph.core.PropertyKey[] properties = vertexLabel.properties().stream()
            .filter(p -> !graphManagement.containsPropertyKey(p)).map(p ->
                graphManagement.getPropertyKey(p)).toList().toArray(new org.janusgraph.core.PropertyKey[0]);
        graphManagement.addProperties(label, properties);
      }
    }

    for (EdgeLabel edgeLabel : graphSchema.edgeLabels()) {
      if (graphManagement.containsVertexLabel(edgeLabel.name())) {
        org.janusgraph.core.EdgeLabel label = graphManagement.getEdgeLabel(edgeLabel.name());
        org.janusgraph.core.PropertyKey[] properties = edgeLabel.properties().stream()
            .filter(p -> !graphManagement.containsPropertyKey(p)).map(p ->
                graphManagement.getPropertyKey(p)).toList().toArray(new org.janusgraph.core.PropertyKey[0]);
        graphManagement.addProperties(label, properties);
      }
    }

    for (VertexIndex index : graphSchema.vertexIndices()) {
      if (!graphManagement.containsGraphIndex(index.name())) {
        List<org.janusgraph.core.PropertyKey> propertyKeys = index.propertyKeys().stream().filter(
            p -> graphManagement.containsPropertyKey(p)
        ).map(
            p -> graphManagement.getPropertyKey(p)
        ).toList();
        IndexBuilder indexBuilder = graphManagement.buildIndex(index.name(), Vertex.class);
        if (index.unique()) {
          indexBuilder.unique();
        }

        for (org.janusgraph.core.PropertyKey propertyKey : propertyKeys) {
          indexBuilder.addKey(propertyKey);
        }

        if (index.type() == "mixed") {
          indexBuilder.buildMixedIndex("search");
        } else {
          indexBuilder.buildCompositeIndex();
        }
      }
    }

    for (EdgeIndex index : graphSchema.edgeIndices()) {
      if (!graphManagement.containsGraphIndex(index.name())) {
        List<org.janusgraph.core.PropertyKey> propertyKeys = index.propertyKeys().stream().filter(
            p -> graphManagement.containsPropertyKey(p)
        ).map(
            p -> graphManagement.getPropertyKey(p)
        ).toList();

        if (graphManagement.containsEdgeLabel(index.edgeLabel()) == false) {
          throw new IllegalArgumentException("Edge label " + index.edgeLabel() + " does not exist");
        }
        org.janusgraph.core.EdgeLabel edgeLabel = graphManagement.getEdgeLabel(index.edgeLabel());
        RelationTypeIndex indexBuilder = graphManagement.buildEdgeIndex(edgeLabel, index.name(), Direction.valueOf(index.direction()),
            Order.valueOf(index.order()), propertyKeys.toArray(new org.janusgraph.core.PropertyKey[0]));
        if (index.unique()) {
          indexBuilder.unique();
        }

        for (org.janusgraph.core.PropertyKey propertyKey : propertyKeys) {
          indexBuilder.addKey(propertyKey);
        }

        if (index.direction() != null) {
          indexBuilder(index.direction());
        }

        if (index.type() == "mixed") {
          indexBuilder.buildMixedIndex("search");
        } else {
          indexBuilder.buildCompositeIndex();
        }
      }
    }

    graphManagement.commit();
  }

  private JanusGraph getJanusGraphManagement(String graphName) {
    Configuration config = loadConfigurationFromFile("src/main/resources/janusgraph-cassandra.properties");
    config.setProperty("storage.cassandra.keyspace", graphName);

    return janusGraphFactory.open(config);
  }

  private Class getClassName(String className) {
    return switch (className.toLowerCase()) {
      case "vertex" -> org.apache.tinkerpop.gremlin.structure.Vertex.class;
      case "edge" -> org.apache.tinkerpop.gremlin.structure.Edge.class;
      default -> throw new IllegalArgumentException("Invalid class name: " + className);
    };
  }

  private static Class getDataType(String dataTypeString) {
    return switch (dataTypeString.toLowerCase()) {
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
  public void dropGraph(String graphName) throws JanusGraphException {
    JanusGraph janusGraph = getJanusGraphManagement(graphName);
    janusGraph.close();
    try {
      JanusGraphFactory.drop(janusGraph);
    } catch (BackendException e) {
      throw new JanusGraphException("Error dropping graph: " + graphName, e);
    }
  }
}
