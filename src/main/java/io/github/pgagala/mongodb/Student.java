package io.github.pgagala.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.UUID;

@Document(collection = "student")
record Student(@Id UUID id,
               String name,
               @Field(name = "email") String mail,
               @DBRef(db = "test") Location location,
               List<Hobby> hobbies) {
}