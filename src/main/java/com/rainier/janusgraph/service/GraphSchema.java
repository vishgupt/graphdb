package com.rainier.janusgraph.service;

import java.util.List;

public record GraphSchema(
    List<PropertyKey> propertyKeys,
    List<VertexLabel> vertexLabels,
    List<EdgeLabel> edgeLabels,
    List<Index> indexes
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
      String description
  ) {

  }

  public record Index(
      String name,
      String type,
      String label,
      List<String> propertyKeys,
      Boolean unique,
      String backend,
      String description
  ) {
  }
}
