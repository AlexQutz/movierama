package com.movierama.controller

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
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class MovieControllerSpec extends Specification {

    MovieService movieService = Mock()
    MovieMapper movieMapper = Mock()
    MovieRepository movieRepository = Mock()

    MovieController controller

    def setup() {
        controller = new MovieController(movieService, movieMapper, movieRepository)
    }

    def "listMovies delegates to service and returns 200 with body"() {
        given:
        def req = new PagingRequest(page: 2, size: 25, sortBy: "title", sortDirection: "ASC")
        def user = new User(id: 10L, username: "alice")
        def page = new PagingResponse<MovieDto>(content: [new MovieDto(id: 1L, title: "A")], page: 2, size: 25, totalElements: 1L)

        when:
        def resp = controller.listMovies(req, user)

        then:
        1 * movieService.getMoviesPageSorted(2, 25, "title", "ASC",_ as User) >> page
        resp instanceof ResponseEntity
        resp.statusCode.value() == 200
        resp.body == page
    }

    def "createMovie throws when duplicate title exists (case-insensitive)"() {
        given:
        def user = new User(id: 10L, username: "alice")
        def dto = new MovieRegistrationDto(title: "   Inception  ")
        movieRepository.findByTitleIgnoreCase("Inception") >> Optional.of(new Movie(id: 99L, title: "Inception"))
        when:
        controller.createMovie(dto, user)
        then:
        def ex = thrown(RuntimeException)
        ex.message == "A movie with the same title already exists"
        0 * movieService.createMovie(_, _)
        0 * movieMapper._
    }

    def "createMovie creates and maps when title unique"() {
        given:
        def user = new User(id: 10L, username: "alice")
        def reg = new MovieRegistrationDto(title: "Arrival")
        def saved = new Movie(id: 1L, title: "Arrival")
        def mapped = new MovieDto(id: 1L, title: "Arrival")
        movieRepository.findByTitleIgnoreCase("Arrival") >> Optional.empty()
        movieService.createMovie(reg, user) >> saved
        movieMapper.toDto(saved) >> mapped
        when:
        def resp = controller.createMovie(reg, user)
        then:
        resp.statusCode.value() == 200
        resp.body == mapped
    }

    def "listByUser uses defaults when PagingRequest is null"() {
        given:
        def userId = 7L
        def user = new User(id: 10L, username: "alice")
        def pageResp = new PagingResponse<MovieDto>(content: [], page: 0, size: 10, totalElements: 0L)
        when:
        def resp = controller.listMoviesByUser(userId, null, user)
        then:
        1 * movieService.getMoviesByUserPaged(userId, _ as Integer, _ as Integer, _ as String, _ as String,_ as User) >> pageResp
        resp.statusCode.value() == 200
        resp.body == pageResp
    }

    def "reactToMovie returns 401 when not authenticated"() {
        when:
        def resp = controller.reactToMovie(5L, "LIKE", null)
        then:
        resp.statusCode.value() == 401
        resp.body == [error: "Authentication required"]
        0 * movieService._
    }

    def "reactToMovie accepts valid reaction and returns 200"() {
        given:
        def user = new User(id: 42L, username: "bob")
        when:
        def resp = controller.reactToMovie(5L, "like", user)
        then:
        1 * movieService.reactToMovie(5L, user, MovieReaction.ReactionType.LIKE)
        resp.statusCode.value() == 200
        resp.body == [message: "Reaction updated successfully"]
    }
}
