package com.movierama.service

import com.movierama.dto.MovieDto
import com.movierama.dto.MovieRegistrationDto
import com.movierama.entity.Movie
import com.movierama.entity.MovieReaction
import com.movierama.entity.User
import com.movierama.mapper.MovieMapper
import com.movierama.paging.PagingResponse
import com.movierama.repository.MovieReactionRepository
import com.movierama.repository.MovieRepository
import org.springframework.data.domain.*
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Unit tests for MovieService with mocked repositories & mapper")
class MovieServiceSpec extends Specification {

    MovieRepository movieRepository = Mock()
    MovieReactionRepository reactionRepository = Mock()
    MovieMapper movieMapper = Mock()

    @Subject
    MovieService service = new MovieService(movieRepository, reactionRepository, movieMapper)

    def "createMovie maps DTO, sets owner, and saves"() {
        given:
        def dto = new MovieRegistrationDto()
        def user = new User(id: 1L, username: "alice")
        def mapped = new Movie(title: "T1")

        when:
        def saved = service.createMovie(dto, user)

        then:
        movieMapper.toEntity(dto) >> mapped
        movieRepository.save(mapped) >> { Movie m ->
            m.id = 10L; return m
        }
        0 * _
        saved.id == 10L
        saved.user.id == 1L
    }

    def "getMoviesPageSorted returns mapped DTOs with paging metadata"() {
        given:
        def m1 = new Movie(id: 1L); def m2 = new Movie(id: 2L)
        def page = new PageImpl<Movie>([m1, m2], PageRequest.of(0, 2, Sort.by("title").ascending()), 5)
        movieRepository.findAll(_ as Pageable) >> page

        and:
        def d1 = new MovieDto(); d1.id = 1L
        def d2 = new MovieDto(); d2.id = 2L
        movieMapper.toDto(m1) >> d1
        movieMapper.toDto(m2) >> d2

        when:
        PagingResponse<MovieDto> resp = service.getMoviesPageSorted(0, 2, "title", "ASC", "ignoredUser")

        then:
        resp.content*.id == [1L, 2L]
        resp.page == 0
        resp.size == 2
        resp.totalElements == 5
        resp.totalPages == 3
        resp.last == false
    }

    def "getMoviesByUserPaged queries repo by user id and maps DTOs"() {
        given:
        def m1 = new Movie(id: 10L); def m2 = new Movie(id: 11L)
        def page = new PageImpl<Movie>([m1, m2], PageRequest.of(1, 2), 4)
        movieRepository.findByUserId(7L, _ as Pageable) >> page

        and:
        def d1 = new MovieDto(id: 10L)
        def d2 = new MovieDto(id: 11L)


        when:
        def resp = service.getMoviesByUserPaged(7L, 1, 2, "createdAt", "DESC")

        then:
        movieMapper.toDto(m1) >> d1
        movieMapper.toDto(m2) >> d2

        and:
        resp.content*.id == [10L, 11L]
        resp.page == 1
        resp.size == 2
        resp.totalElements == 4
        resp.totalPages == 2
    }

    def "reactToMovie creates a new reaction when none exists"() {
        given:
        def owner = new User(id: 1L)
        def movie = new Movie(id: 100L, user: owner)
        def reactor = new User(id: 2L)

        and:
        movieRepository.findById(100L) >> Optional.of(movie)
        reactionRepository.findByUserIdAndMovieId(2L, 100L) >> Optional.empty()

        when:
        service.reactToMovie(100L, reactor, MovieReaction.ReactionType.LIKE)

        then:
        1 * reactionRepository.save({ MovieReaction r ->
            assert r.movie.id == 100L
            assert r.user.id == 2L
            assert r.reactionType == MovieReaction.ReactionType.LIKE
            true
        })
        0 * reactionRepository.delete(_)
    }

    def "reactToMovie toggles (deletes) when same reaction is sent again"() {
        given:
        def owner = new User(id: 1L)
        def movie = new Movie(id: 101L, user: owner)
        def reactor = new User(id: 2L)
        def existing = new MovieReaction(id: 5L, movie: movie, user: reactor, reactionType: MovieReaction.ReactionType.LIKE)

        and:
        movieRepository.findById(101L) >> Optional.of(movie)
        reactionRepository.findByUserIdAndMovieId(2L, 101L) >> Optional.of(existing)

        when:
        service.reactToMovie(101L, reactor, MovieReaction.ReactionType.LIKE)

        then:
        1 * reactionRepository.delete(existing)
        0 * reactionRepository.save(_)
    }

    def "reactToMovie updates when different reaction is sent"() {
        given:
        def owner = new User(id: 1L)
        def movie = new Movie(id: 102L, user: owner)
        def reactor = new User(id: 2L)
        def existing = new MovieReaction(id: 6L, movie: movie, user: reactor, reactionType: MovieReaction.ReactionType.LIKE)

        when:
        service.reactToMovie(102L, reactor, MovieReaction.ReactionType.HATE)

        then:
        movieRepository.findById(102L) >> Optional.of(movie)
        reactionRepository.findByUserIdAndMovieId(2L, 102L) >> Optional.of(existing)
        1 * reactionRepository.save({ MovieReaction r ->
            assert r.is(existing)
            assert r.reactionType == MovieReaction.ReactionType.HATE
            true
        })
        0 * reactionRepository.delete(_)
    }

    def "reactToMovie throws when user reacts to own movie"() {
        given:
        def owner = new User(id: 9L)
        def movie = new Movie(id: 200L, user: owner)

        and:
        movieRepository.findById(200L) >> Optional.of(movie)

        when:
        service.reactToMovie(200L, owner, MovieReaction.ReactionType.LIKE)

        then:
        def ex = thrown(RuntimeException)
        ex.message == "You cannot react to your own movie"
        0 * reactionRepository._
    }
}
