package io.github.pgagala.mongodb


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import static java.util.UUID.randomUUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application])
@Slf4j
@RandomSingleSpecOrder
class MongoDbInteractionSpec extends Specification {

    @Shared
    private String uri

    def setupSpec() {
        MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
        mongoDBContainer.setPortBindings(["49999:27017"])
        mongoDBContainer.start()

        def port = mongoDBContainer.getMappedPort(27017).toString()
        def host = "localhost"
        def db = "test"
        System.setProperty("spring.data.mongodb.port", port)
        System.setProperty("spring.data.mongodb.host", host)
        System.setProperty("spring.data.mongodb.database", "test")
        System.setProperty("spring.data.mongodb.replica-set-name", "docker-rs")
        uri = mongoDBContainer.getReplicaSetUrl()

        log.info("Starting mongoDb with name: $db on $host with port: $port")
    }

    private static final String NAME = "some-student"

    @Autowired
    StudentRepository studentRepository

    @Autowired
    SimpleStudentRepository simpleStudentRepository

    @Autowired
    LocationRepository locationRepository

    @Autowired
    MongoTransactionManager mongoTransactionManager

    @Autowired
    MongoTemplate mongoTemplate

    def "added students in one batch should be returned"() {
        given: "students"
            def students = students()

        when: "students are added"
            studentRepository.insert(students)
        then: "they should be returned"
            studentRepository.findAll().containsAll(students)
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

    def "added students can be removed in one batch"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        when: "students are removed"
            studentRepository.deleteAll()

        then: "non students should be returned"
            studentRepository.findAll().isEmpty()
    }

    def "added students can be removed one by one"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        when: "students are removed"
            studentRepository.delete(students[0])
            studentRepository.delete(students[1])

        then: "non students should be returned"
            studentRepository.findAll().find {
                it.id() == students[0].id()
            } == null
            studentRepository.findAll().find {
                it.id() == students[1].id()
            } == null
    }

    def "added students can be retrieved by id"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        expect: "students can be retrieved by ids"
            studentRepository.findById(students[0].id()) != null
            studentRepository.findById(students[1].id()) != null
    }

    def "existing student in db can be updated"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        when: "student is updated"
            def newMail = "newMail-${randomUUID()}-gmail.com"
            def updatedStudent1 = new Student(students[0].id(), students[0].name(), newMail, null, [])
            studentRepository.save(updatedStudent1)

        then: "updated student exists in db"
            studentRepository.findAll().find {
                it.id() == students[0].id()
                        && it == updatedStudent1
            }
        and: "not updated student added earlier exists without any changes"
            studentRepository.findAll().find {
                it.id() == students[1].id()
                        && it == students[1]
            }
    }

    def "student should be retrieved by name"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        expect: "student can be retrieved by name"
            studentRepository.findByName(students[0].name()).size() == 1
            studentRepository.findByName(students[0].name())[0] == students[0]
    }

    def "student should be retrieved by hobby"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        expect: "student can be retrieved by hobby"
            studentRepository.findByHobbiesContains(students[0].hobbies()[0]).size() >= 1
            studentRepository.findByHobbiesName(students[0].hobbies()[0].name()).size() >= 1
            studentRepository.findByHobbiesNameStartsWith("skii").size() >= 1
            studentRepository.findByHobbiesNameLike("iing").size() >= 1
    }

    def "fetch students with pagination should be possible"() {
        given: "added students"
            def students = students()
            studentRepository.insert(students)

        expect: "requested page with size 1 should return single student"
            studentRepository.findAll(PageRequest.of(0, 1)).size() == 1
    }

    def "student with reference to other collection should be saved"() {
        given: "location"
            def location = new Location(randomUUID(), "gdansk", "80-111")
        and: "student referring to location"
            def student1Name = randomize(NAME)
            def student = new Student(randomUUID(), student1Name, "$student1Name@protonmail.com", location, [new Hobby("skiing")])

        when: "location is saved"
            locationRepository.save(location)
        and: "student is saved:"
            studentRepository.save(student)

        then: "student with location should be retrieved"
            studentRepository.findAll().find {
                it.id() == student.id()
                        && it == student
            }
        and: "location's collection should exist"
            mongoTemplate.collectionExists(Location)
    }

    def "student should be find by reference to other collection"() {
        given: "student with location"
            def location = new Location(randomUUID(), "gdansk-${randomUUID()}", "80-111")
            def student1Name = randomize(NAME)
            def student = new Student(randomUUID(), student1Name, "$student1Name@protonmail.com", location, [new Hobby("skiing")])

        when: "student is saved:"
            locationRepository.save(location)
            studentRepository.save(student)

        then: "student should be retrieved by location id"
            studentRepository.findByLocationId(location.id()).size() == 1
            studentRepository.findByLocationId(location.id()).find {
                it.id() == student.id()
                        && it == student
            }
    }

    def "multiple versions of student (with different fields) can be maintained in the same collection"() {
        given: "students with different version"
            def studentV1 = students()[0]
            def studentV2 = new SimpleStudent(randomUUID(), randomize(NAME))

        when: "students are added"
            studentRepository.save(studentV1)
            simpleStudentRepository.save(studentV2)

        then: "students with different versions can be retrieved from the same collection"
            studentRepository.findAll().findAll {
                it.id() == studentV1.id()
                        && it == studentV1
            }.size() == 1
            simpleStudentRepository.findAll().findAll {
                it.id() == studentV2.id()
                        && it == studentV2
            }.size() == 1
    }

    def "native query should return same result as spring data repository"() {
        given: "students"
            def students = students()

        when: "students are added"
            studentRepository.insert(students)
        then: "they should be returned"
            studentRepository.findAll() == studentRepository.findAllNative()
            studentRepository.findByName(students[0].name()) == studentRepository.findByNameNative(students[0].name())
    }

    def "rollback of transaction should work"() {
        given: "transaction template"
            TransactionTemplate transactionTemplate = new TransactionTemplate(mongoTransactionManager)
        and: "existing student collection (required for multi-document transaction)"
            if (!mongoTemplate.collectionExists(Student))
                mongoTemplate.createCollection(Student)
        and: "students"
            def students = students()

        when: "error occurred during performing multiple insert operations in one transaction"
            try {
                transactionTemplate.execute({
                    mongoTemplate.insert(students[0])
                    mongoTemplate.insert(students[1])
                    throw new RuntimeException()
                })
            }
            catch (Exception ignored) {
            }

        then: "in database there aren't any records added"
            studentRepository.findAll().find {
                it.id() == students[0].id()
            } == null
            studentRepository.findAll().find {
                it.id() == students[1].id()
            } == null
    }

    def "cannot perform insert operations in transaction for non existing collection"() {
        given: "transaction template"
            TransactionTemplate transactionTemplate = new TransactionTemplate(mongoTransactionManager)
        and: "lack of student collection"
            mongoTemplate.dropCollection(Student)
        and: "students"
            def students = students()

        when: "performing multiple insert operations on non existing collection in one transaction"
            try {
                transactionTemplate.execute({
                    mongoTemplate.insert(students[0])
                    mongoTemplate.insert(students[1])
                })
            }
            catch (Exception ignored) {
            }

        then: "in database there aren't any records added"
            studentRepository.findAll().find {
                it.id() == students[0].id()
                        && it.name() == students[0].name()
            } == null
            studentRepository.findAll().find {
                it.id() == students[1].id()
                        && it.name() == students[1].name()
            } == null
    }

    static def students() {
        def student1Name = randomize(NAME)
        def student1 = new Student(randomUUID(), student1Name, "$student1Name@protonmail.com", null, [new Hobby("skiing")])
        def student2Name = randomize(NAME)
        def student2 = new Student(randomUUID(), student2Name, "$student2Name@protonmail.com", null, [new Hobby("swimming"), new Hobby("reading")])
        return [student1, student2]
    }

    static def randomize(String field) {
        return "$field-${randomUUID()}"
    }
}