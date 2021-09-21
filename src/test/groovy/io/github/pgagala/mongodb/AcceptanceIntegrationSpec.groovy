package io.github.pgagala.mongodb


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

import static java.util.UUID.randomUUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application])
class AcceptanceIntegrationSpec extends Specification {

    def setupSpec() {
        MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
        mongoDBContainer.start()

        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString())
        System.setProperty("spring.data.mongodb.host", "localhost")
        System.setProperty("spring.data.mongodb.database", "test")
        System.setProperty("spring.data.mongodb.replica-set-name", "docker-rs")
    }

    private static final String NAME = "some-student"

    @Autowired
    StudentRepository studentRepository

    def "added students in one batch should be returned"() {
        given: "students"
            def students = students()

        when: "students are added"
            studentRepository.insert(students)
        then: "they should be returned"
            studentRepository.findAll() == students
    }

    def "added students one by one should be returned"() {
        given: "students"
            def students = students()

        when: "students are added"
            studentRepository.insert(students[0])
            studentRepository.insert(students[1])
        then: "they should be returned"
            studentRepository.findAll().containsAll(students)
    }

    static def students() {
        def student1Name = randomize(NAME)
        def student1 = new Student(randomUUID(), student1Name, "$student1Name@protonmail.com", [new Hobby("skiing")])
        def student2Name = randomize(NAME)
        def student2 = new Student(randomUUID(), student2Name, "$student2Name@protonmail.com", [new Hobby("swimming"), new Hobby("reading")])
        return [student1, student2]
    }

    static def randomize(String field) {
        return "$field-${randomUUID()}"
    }
}