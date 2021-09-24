package io.github.pgagala.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SimpleStudentRepository extends MongoRepository<SimpleStudent, UUID> {
}