package com.rainier.janusgraph.service;

import java.util.List;
import org.locationtech.jts.geomgraph.Edge;

public record GraphSchema(
    List<PropertyKey> propertyKeys,
    List<VertexLabel> vertexLabels,
    List<EdgeLabel> edgeLabels,
    List<VertexIndex> vertexIndices,
    List<EdgeIndex> edgeIndices
) {

  public record PropertyKey(
      String name,
      String dataType,
      String cardinality,
      String description,
      Boolean unique
  ) {

  }

  public record VertexLabel(
      String name,
      List<String> properties,
      String description
  ) {

  }

  public record EdgeLabel(
      String name,
      String multiplicity,
      List<String> properties,
      List<Connection> connections,
      boolean directed,
      String description
  ) {

  }

  public record Connection(
      String from,
      String to
  ) {

  }

  public record VertexIndex(
      String name,
      String type,
      String vertexLabel,
      List<String> propertyKeys,
      Boolean unique,
      String description
  ) {

  }

  public record EdgeIndex(
      String name,
      String type,
      String edgeLabel,
      List<String> propertyKeys,
      Boolean unique,
      String direction,
      String order,
      String description
  ) {
  }
}
