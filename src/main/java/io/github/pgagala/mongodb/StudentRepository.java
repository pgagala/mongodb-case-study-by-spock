package io.github.pgagala.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentRepository extends MongoRepository<Student, UUID> {
    List<Student> findByName(String name);

    List<Student> findByHobbiesContains(Hobby hobby);
    List<Student> findByHobbiesName(String hobbyName);
    List<Student> findByHobbiesNameStartsWith(String hobbyName);
    List<Student> findByHobbiesNameLike(String hobbyName);
}