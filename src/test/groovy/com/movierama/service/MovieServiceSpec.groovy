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
        1 * movieMapper.toEntity(dto) >> mapped
        1 * movieRepository.save(mapped) >> { Movie m ->
            m.id = 10L; return m
        }

        and:
        0 * _
        saved.id == 10L
        saved.user.id == 1L
    }

    def "getMoviesPageSorted returns mapped DTOs with paging metadata (property sort, no user)"() {
        given:
        def m1 = new Movie(id: 1L); def m2 = new Movie(id: 2L)
        def pageable = PageRequest.of(0, 2, Sort.by("title").ascending())
        def page = new PageImpl<Movie>([m1, m2], pageable, 5)

        and: "mapper without user context is used"
        def d1 = new MovieDto(id: 1L)
        def d2 = new MovieDto(id: 2L)

        when:
        PagingResponse<MovieDto> resp = service.getMoviesPageSorted(0, 2, "title", "ASC", null)

        then:
        1 * movieRepository.findAll({ Pageable p -> p.sort == Sort.by("title").ascending() }) >> page
        1 * movieMapper.toDto(m1) >> d1
        1 * movieMapper.toDto(m2) >> d2

        and:
        resp.content*.id == [1L, 2L]
        resp.page == 0
        resp.size == 2
        resp.totalElements == 5
        resp.totalPages == 3
        !resp.last
        0 * _
    }

    // ---------------- getMoviesPageSorted (default sort by id when sortBy empty)

    def "getMoviesPageSorted uses default sort by id ASC when sortBy is empty"() {
        given:
        def m1 = new Movie(id: 3L); def m2 = new Movie(id: 5L)
        def pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"))
        def page = new PageImpl<Movie>([m1, m2], pageable, 4)

        when:
        def resp = service.getMoviesPageSorted(0, 2, "", "ASC", null)

        then:
        1 * movieRepository.findAll({ Pageable p -> p.sort == Sort.by(Sort.Direction.ASC, "id") }) >> page
        1 * movieMapper.toDto(m1) >> new MovieDto(id: 3L)
        1 * movieMapper.toDto(m2) >> new MovieDto(id: 5L)

        and:
        resp.content*.id == [3L, 5L]
        resp.totalElements == 4
        resp.totalPages == 2
        0 * _
    }

    def "getMoviesPageSorted reaction sort uses Specification and passes current user to mapper"() {
        given:
        def currentUser = new User(id: 42L, username: "bob")
        def m1 = new Movie(id: 10L)
        def unsorted = PageRequest.of(1, 2)
        def page = new PageImpl<Movie>([m1], unsorted, 3)
        when:
        PagingResponse<MovieDto> resp = service.getMoviesPageSorted(1, 2, "likeCount", "DESC", currentUser)

        then:
        1 * movieRepository.findAll(_ as org.springframework.data.jpa.domain.Specification<Movie>, { Pageable p ->
            !p.sort.isSorted() && p.pageNumber == 1 && p.pageSize == 2
        }) >> page
        1 * movieMapper.toDto(m1, 42L) >> new MovieDto(id: 10L, userLiked: true, userHated: false)

        and:
        resp.content*.id == [10L]
        resp.page == 1
        resp.size == 2
        resp.totalElements == 3
        resp.totalPages == 2
        0 * _
    }

    def "getMoviesByUserPaged queries repo by user id and maps DTOs (with user context)"() {
        given:
        def requestingUser = new User(id: 77L)
        def m1 = new Movie(id: 10L); def m2 = new Movie(id: 11L)
        def pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
        def page = new PageImpl<Movie>([m1, m2], pageable, 4)

        when:
        def resp = service.getMoviesByUserPaged(7L, 1, 2, "createdAt", "DESC", requestingUser)

        then:
        1 * movieRepository.findByUserId(7L, { Pageable p ->
            p.pageNumber == 1 && p.pageSize == 2 && p.sort == Sort.by(Sort.Direction.DESC, "createdAt")
        }) >> page
        1 * movieMapper.toDto(m1, 77L) >> new MovieDto(id: 10L)
        1 * movieMapper.toDto(m2, 77L) >> new MovieDto(id: 11L)

        and:
        resp.content*.id == [10L, 11L]
        resp.page == 1
        resp.size == 2
        resp.totalElements == 4
        resp.totalPages == 2
        0 * _
    }

    def "getMoviesByUserPaged uses default id ASC when sortBy is empty"() {
        given:
        def m1 = new Movie(id: 1L); def m2 = new Movie(id: 2L)
        def pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"))
        def page = new PageImpl<Movie>([m1, m2], pageable, 2)

        when:
        def resp = service.getMoviesByUserPaged(9L, 0, 2, "", "ASC", null)

        then:
        1 * movieRepository.findByUserId(9L, { Pageable p ->
            p.pageNumber == 0 && p.pageSize == 2 && p.sort == Sort.by(Sort.Direction.ASC, "id")
        }) >> page
        1 * movieMapper.toDto(m1) >> new MovieDto(id: 1L)
        1 * movieMapper.toDto(m2) >> new MovieDto(id: 2L)

        and:
        resp.content*.id == [1L, 2L]
        resp.totalElements == 2
        resp.totalPages == 1
        0 * _
    }

    def "reactToMovie creates a new reaction when none exists"() {
        given:
        def owner = new User(id: 1L)
        def movie = new Movie(id: 100L, user: owner)
        def reactor = new User(id: 2L)

        when:
        service.reactToMovie(100L, reactor, MovieReaction.ReactionType.LIKE)

        then:
        1 * movieRepository.findById(100L) >> Optional.of(movie)
        1 * reactionRepository.findByUserIdAndMovieId(2L, 100L) >> Optional.empty()

        and:
        1 * reactionRepository.save({ MovieReaction r ->
            assert r.movie.id == 100L
            assert r.user.id == 2L
            assert r.reactionType == MovieReaction.ReactionType.LIKE
            true
        })
        0 * reactionRepository.delete(_)
        0 * _
    }

    def "reactToMovie toggles (deletes) when same reaction is sent again"() {
        given:
        def owner = new User(id: 1L)
        def movie = new Movie(id: 101L, user: owner)
        def reactor = new User(id: 2L)
        def existing = new MovieReaction(id: 5L, movie: movie, user: reactor, reactionType: MovieReaction.ReactionType.LIKE)

        when:
        service.reactToMovie(101L, reactor, MovieReaction.ReactionType.LIKE)

        then:
        1 * movieRepository.findById(101L) >> Optional.of(movie)
        1 * reactionRepository.findByUserIdAndMovieId(2L, 101L) >> Optional.of(existing)

        and:
        1 * reactionRepository.delete(existing)
        0 * reactionRepository.save(_)
        0 * _
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
        1 * movieRepository.findById(102L) >> Optional.of(movie)
        1 * reactionRepository.findByUserIdAndMovieId(2L, 102L) >> Optional.of(existing)

        and:
        1 * reactionRepository.save({ MovieReaction r ->
            assert r.is(existing)
            assert r.reactionType == MovieReaction.ReactionType.HATE
            true
        })
        0 * reactionRepository.delete(_)
        0 * _
    }

    def "reactToMovie throws when user reacts to own movie"() {
        given:
        def owner = new User(id: 9L)
        def movie = new Movie(id: 200L, user: owner)

        when:
        service.reactToMovie(200L, owner, MovieReaction.ReactionType.LIKE)

        then:
        1 * movieRepository.findById(200L) >> Optional.of(movie)

        and:
        def ex = thrown(RuntimeException)
        ex.message == "You cannot react to your own movie"
        0 * reactionRepository._
        0 * _
    }

    def "reactToMovie throws when movie not found"() {
        given:
        def reactor = new User(id: 3L)

        when:
        service.reactToMovie(404L, reactor, MovieReaction.ReactionType.HATE)

        then:
        1 * movieRepository.findById(404L) >> Optional.empty()

        and:
        def ex = thrown(RuntimeException)
        ex.message == "Movie not found"
        0 * reactionRepository._
        0 * _
    }
}
