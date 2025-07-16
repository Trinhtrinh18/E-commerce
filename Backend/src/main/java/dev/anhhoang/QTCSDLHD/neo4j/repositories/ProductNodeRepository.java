package dev.anhhoang.QTCSDLHD.neo4j.repositories;

import dev.anhhoang.QTCSDLHD.neo4j.entities.ProductNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductNodeRepository extends Neo4jRepository<ProductNode, String> {
}