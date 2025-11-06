package com.movierama.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.movierama.BaseSpecification
import com.movierama.dto.UserRegistrationDto
import com.movierama.security.jwt.JwtTokenProvider
import com.movierama.service.ProfileService
import com.movierama.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.User
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc

import static org.mockito.ArgumentMatchers.any
import static org.mockito.BDDMockito.given
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class AuthControllerITSpec extends BaseSpecification {

    @Autowired MockMvc mockMvc
    @Autowired ObjectMapper objectMapper

    @MockitoBean UserService userService
    @MockitoBean AuthenticationManager authenticationManager
    @MockitoBean JwtTokenProvider jwtTokenProvider
    @MockitoBean ProfileService profileService

    private String toJson(Object o) { objectMapper.writeValueAsString(o) }

    def "POST /api/auth/register returns success string"() {
        given:
        def dto = new UserRegistrationDto()
        dto.setUsername("alice")
        dto.setPassword("secret")
        and:
        // No return expected; just ensure it's invoked
        given(userService.registerUser(dto)).willAnswer { null }
        expect:
        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto))
        )
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"))
    }

    def "POST /api/auth/login authenticates and returns token JSON"() {
        given:
        def login = new AuthController.LoginRequest(username: "alice", password: "secret")
        def principal = new User("alice", "secret", [])
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        and:
        given(authenticationManager.authenticate(any(Authentication))).willReturn(auth)
        given(jwtTokenProvider.generateToken(principal)).willReturn("jwt-abc-123")
        expect:
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(login))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.token').value("jwt-abc-123"))
    }

           def "POST /api/auth/login bubbles auth failures"() {
               given:
               def login = new AuthController.LoginRequest(username: "alice", password: "bad")
               and:
               given(authenticationManager.authenticate(any(Authentication))).willAnswer {
                   throw new AuthenticationException() {}
               }
               expect:
               mockMvc.perform(
                       post("/api/auth/login")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(toJson(login))
               )
                       .andExpect(status().isInternalServerError())
           }
}
