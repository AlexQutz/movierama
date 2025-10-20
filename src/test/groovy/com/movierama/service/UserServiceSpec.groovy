package com.movierama.service

import com.movierama.dto.UserRegistrationDto
import com.movierama.entity.User
import com.movierama.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title


class UserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    PasswordEncoder passwordEncoder = Mock()

    @Subject
    UserService service = new UserService(userRepository, passwordEncoder)

    def "registerUser throws when username already exists"() {
        given:
        def dto = new UserRegistrationDto(
                username: "jdoe",
                email: "john@doe.com",
                password: "secret",
                firstName: "John",
                lastName: "Doe"
        )


        when:
        service.registerUser(dto)

        then:
        userRepository.existsByUsername("jdoe") >> true

        and:
        def ex = thrown(RuntimeException)
        ex.message == "Username already exists"
        0 * _
    }

    def "registerUser throws when email already exists"() {
        given:
        def dto = new UserRegistrationDto(
                username: "jdoe",
                email: "john@doe.com",
                password: "secret",
                firstName: "John",
                lastName: "Doe"
        )

        when:
        service.registerUser(dto)

        then:
        userRepository.existsByUsername("jdoe") >> false
        userRepository.existsByEmail("john@doe.com") >> true

        and:
        def ex = thrown(RuntimeException)
        ex.message == "Email already exists"
        0 * _
    }

    def "registerUser persists user with encoded password and USER role"() {
        given:
        def dto = new UserRegistrationDto(
                username: "jdoe",
                email: "john@doe.com",
                password: "secret",
                firstName: "John",
                lastName: "Doe"
        )

        when:
        def saved = service.registerUser(dto)

        then: "we save a User with encoded password and correct fields"
        1 * userRepository.save(_ as User) >> { User u ->
            assert u.username == "jdoe"
            assert u.email == "john@doe.com"
            assert u.password == "ENC(secret)"
            assert u.firstName == "John"
            assert u.lastName == "Doe"
            assert u.role == User.Role.USER
            return u // return what repository would return
        }
        userRepository.existsByUsername("jdoe") >> false
        userRepository.existsByEmail("john@doe.com") >> false
        passwordEncoder.encode("secret") >> "ENC(secret)"
        0 * _

        and:
        saved.password == "ENC(secret)"
        saved.role == User.Role.USER
    }

    def "findByUsername returns user when found"() {
        given:
        def u = new User(id: 1L, username: "jdoe")
        userRepository.findByUsername("jdoe") >> Optional.of(u)

        expect:
        service.findByUsername("jdoe").is(u)
    }

    def "findByUsername throws when not found"() {
        given:
        userRepository.findByUsername("missing") >> Optional.empty()

        when:
        service.findByUsername("missing")

        then:
        thrown(UsernameNotFoundException)
    }

    def "findById returns user when found"() {
        given:
        def u = new User(id: 42L, username: "jdoe")
        userRepository.findById(42L) >> Optional.of(u)

        expect:
        service.findById(42L).is(u)
    }

    def "findById throws when not found"() {
        given:
        userRepository.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        def ex = thrown(RuntimeException)
        ex.message == "User not found with id: 99"
    }
}
