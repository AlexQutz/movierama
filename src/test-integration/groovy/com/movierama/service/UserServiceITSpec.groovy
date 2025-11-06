package com.movierama.service

import com.movierama.BaseSpecification
import com.movierama.dto.UserRegistrationDto
import com.movierama.entity.User
import com.movierama.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceITSpec extends BaseSpecification {

    @Autowired
    UserService userService

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder


    def "registerUser saves encoded password and default role, retrievable by id and username"() {
        given:
        def dto = regDto("jdoe", "john@doe.com", "secret", "John", "Doe")

        when:
        User saved = userService.registerUser(dto)

        then:
        saved.id != null
        saved.username == "jdoe"
        saved.email == "john@doe.com"
        saved.role == User.Role.USER

        and: "password is encoded and matches the raw"
        saved.password != "secret"
        passwordEncoder.matches("secret", saved.password)

        and: "we can fetch by username and id"
        userService.findByUsername("jdoe").id == saved.id
        userService.findById(saved.id).username == "jdoe"
    }

    def "registerUser rejects duplicate username"() {
        given:
        userService.registerUser(regDto("jdoe", "john@doe.com", "secret", "John", "Doe"))

        when:
        userService.registerUser(regDto("jdoe", "another@doe.com", "x", "Jane", "Roe"))

        then:
        def ex = thrown(RuntimeException)
        ex.message == "Username already exists"
    }

    def "registerUser rejects duplicate email"() {
        given:
        userService.registerUser(regDto("jdoe", "john@doe.com", "secret", "John", "Doe"))

        when:
        userService.registerUser(regDto("another", "john@doe.com", "x", "Jane", "Roe"))

        then:
        def ex = thrown(RuntimeException)
        ex.message == "Email already exists"
    }

    def "findByUsername throws UsernameNotFoundException when missing"() {
        when:
        userService.findByUsername("missing")

        then:
        thrown(UsernameNotFoundException)
    }

    // --- helper ---
    private static UserRegistrationDto regDto(String username, String email, String password, String first, String last) {
        new UserRegistrationDto(
                username: username,
                email: email,
                password: password,
                firstName: first,
                lastName: last
        )
    }

}
