package com.movierama.controller

import com.movierama.dto.UserRegistrationDto
import com.movierama.security.jwt.JwtTokenProvider
import com.movierama.service.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import spock.lang.Specification

class AuthControllerSpec extends Specification {

    UserService userService = Mock()
    AuthenticationManager authenticationManager = Mock()
    JwtTokenProvider jwtTokenProvider = Mock()

    AuthController controller

    def setup() {
        controller = new AuthController(userService, authenticationManager, jwtTokenProvider)
    }

    def "register delegates to service and returns success message"() {
        given:
        def dto = new UserRegistrationDto()
        dto.setUsername("alice")
        dto.setPassword("secret")
        when:
        def resp = controller.register(dto)
        then:
        1 * userService.registerUser(dto)
        resp == "User registered successfully"
    }

    def "login authenticates and returns token response"() {
        given:
        def principal = new User("alice", "secret", [])
        def auth = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        def req = new AuthController.LoginRequest(username: "alice", password: "secret")
        when:
        def resp = controller.login(req)
        then:
        1 * authenticationManager.authenticate({ Authentication a ->
            a.principal == "alice" && a.credentials == "secret"
        } as Authentication) >> auth
        1 * jwtTokenProvider.generateToken(principal) >> "jwt-123"
        resp.token == "jwt-123"
    }
}
