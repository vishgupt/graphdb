package com.rainier;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;

public class ApplicationMain {

  public static void main(String[] args) {
    // Open the JanusGraph instance
    JanusGraph graph = JanusGraphFactory.open("src/main/resources/janusgraph-cassandra.properties");

    try {
      // Add a vertex
      Vertex person = graph.addVertex("person");
      person.property("name", "Alice");
      person.property("age", 29);

      // Add another vertex and create an edge
      Vertex software = graph.addVertex("software");
      software.property("name", "JanusGraph");

      person.addEdge("uses", software);

      // Commit the transaction
      graph.tx().commit();

      // Query the graph
      graph.traversal().V().has("name", "Alice").forEachRemaining(v -> {
        System.out.println("Found vertex: " + v.property("name").value());
      });

    } finally {
      // Close the graph connection
      graph.close();
    }
  }
}