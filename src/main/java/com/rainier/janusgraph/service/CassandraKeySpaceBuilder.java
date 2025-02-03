package com.rainier.janusgraph.service;

import com.datastax.oss.driver.api.core.CqlSession;
import groovy.util.logging.Slf4j;

@Slf4j
public class CassandraKeySpaceBuilder implements KeySpaceBuilder {

  @Override
  public void build(String keyspace) {
    try (CqlSession session = CqlSession.builder()
        .withKeyspace("system")  // Default keyspace for administration
        .build()) {

      // Define the keyspace creation query
      String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
          " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};";

      // Execute the query
      session.execute(createKeyspaceQuery);
    } catch (Exception e) {
      throw new JanusGraphException("Error creating keyspace: " + keyspace, e);
    }
  }
}
