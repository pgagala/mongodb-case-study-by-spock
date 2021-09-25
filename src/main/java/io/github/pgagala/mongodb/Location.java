package io.github.pgagala.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "location")
record Location(@Id UUID id, String city, String postalCode) {
}