package com.movierama.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.movierama.dto.MovieDto
import com.movierama.dto.MovieRegistrationDto
import com.movierama.entity.Movie
import com.movierama.entity.MovieReaction
import com.movierama.entity.User
import com.movierama.mapper.MovieMapper
import com.movierama.paging.PagingRequest
import com.movierama.paging.PagingResponse
import com.movierama.repository.MovieRepository
import com.movierama.service.MovieService
import com.movierama.service.ProfileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Stepwise

import static org.mockito.ArgumentMatchers.*
import static org.mockito.BDDMockito.given
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = MovieController)
@AutoConfigureMockMvc(addFilters = false) // disable Spring Security filters since we test controller logic
@Stepwise
class MovieControllerITSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired ObjectMapper objectMapper

    @MockitoBean MovieService movieService
    @MockitoBean MovieMapper movieMapper
    @MockitoBean MovieRepository movieRepository
    @MockitoBean ProfileService profileService

    private String toJson(Object o) { objectMapper.writeValueAsString(o) }

    def "POST /api/movies returns paged movies with 200"() {
        given:
        def req = new PagingRequest(page: 1, size: 5, sortBy: "title", sortDirection: "DESC")
        def dto = new MovieDto(id: 3L, title: "Zootopia")
        def page = new PagingResponse<MovieDto>(content: [dto], page: 1, size: 5, totalElements: 1L)
        given(movieService.getMoviesPageSorted(1, 5, "title", "DESC", any(User.class))).willReturn(page)
        expect:
        mockMvc.perform(
                post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.page').value(1))
                .andExpect(jsonPath('$.size').value(5))
                .andExpect(jsonPath('$.content[0].id').value(3))
                .andExpect(jsonPath('$.content[0].title').value("Zootopia"))
    }

    def "POST /api/secured/movies rejects duplicate title via repository check"() {
        given:
        def reg = new MovieRegistrationDto(title: "Inception")
        given(movieRepository.findByTitleIgnoreCase("Inception")).willReturn(Optional.of(new Movie(id: 7L, title: "Inception")))
        expect:
        mockMvc.perform(
                post("/api/secured/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(reg))
                        .principal(new UsernamePasswordAuthenticationToken(new User(id: 10L, username: "alice"), null))
        )
                .andExpect(status().isInternalServerError()) // RuntimeException bubbles; if you add @ControllerAdvice, update this accordingly
    }

           def "POST /api/secured/movies creates movie and maps to DTO"() {
               given:
               def reg = new MovieRegistrationDto(title: "Arrival")
               def saved = new Movie(id: 1L, title: "Arrival")
               def dto = new MovieDto(id: 1L, title: "Arrival")
               given(movieRepository.findByTitleIgnoreCase("Arrival")).willReturn(Optional.empty())
               given(movieService.createMovie(any(MovieRegistrationDto), any(User))).willReturn(saved)
               given(movieMapper.toDto(saved)).willReturn(dto)
               expect:
               mockMvc.perform(
                       post("/api/secured/movies")
                               .contentType(MediaType.APPLICATION_JSON)
                               .content(toJson(reg))
                               .principal(new UsernamePasswordAuthenticationToken(new User(id: 11L, username: "bob"), null))
               )
                       .andExpect(status().isOk())
                       .andExpect(jsonPath('$.id').value(1))
                       .andExpect(jsonPath('$.title').value("Arrival"))
           }

    def "POST /api/secured/movies/user/{userId} defaults paging when body is null"() {
        given:
        def userId = 77L
        def response = new PagingResponse<MovieDto>(content: [], page: 0, size: 10, totalElements: 0L)
        given(movieService.getMoviesByUserPaged(eq(userId), anyInt(), anyInt(), anyString(), anyString(), any(User.class)))
                .willReturn(response)
        expect:
        mockMvc.perform(
                post("/api/secured/movies/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(new UsernamePasswordAuthenticationToken(new User(id: 1L, username: "sue"), null))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.total').value(0))
    }

    def "POST /api/secured/movies/{id}/react returns 401 when principal missing"() {
        expect:
        mockMvc.perform(
                post("/api/secured/movies/{movieId}/react", 5L)
                        .param("reaction", "LIKE")
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Authentication required"))
    }

    def "POST /api/secured/movies/{id}/react handles invalid reaction"() {
        given:
        def principal = new UsernamePasswordAuthenticationToken(new User(id: 5L, username: "neo"), null)
        expect:
        mockMvc.perform(
                post("/api/secured/movies/{movieId}/react", 9L)
                        .param("reaction", "meh")
                        .principal(principal)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid reaction type"))
    }

    def "POST /api/secured/movies/{id}/react ok path"() {
        given:
        def principal = new UsernamePasswordAuthenticationToken(new User(id: 5L, username: "neo"), null)
        expect:
        mockMvc.perform(
                post("/api/secured/movies/{movieId}/react", 9L)
                        .param("reaction", "DISLIKE")
                        .principal(principal)
        )
                .andExpect(status().isOk())
                .andExpect(content().string("Reaction updated successfully"))
    }

    def "POST /api/secured/movies/{id}/react maps service runtime error to 400"() {
        given:
        def principal = new UsernamePasswordAuthenticationToken(new User(id: 5L, username: "neo"), null)
        given(movieService.reactToMovie(eq(12L), any(User), eq(MovieReaction.ReactionType.LIKE)))
                .willAnswer { throw new RuntimeException("Already reacted") }
        expect:
        mockMvc.perform(
                post("/api/secured/movies/{movieId}/react", 12L)
                        .param("reaction", "LIKE")
                        .principal(principal)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Already reacted"))
    }
}
