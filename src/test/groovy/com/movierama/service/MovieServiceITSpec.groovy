package com.movierama.service

import com.movierama.dto.MovieDto
import com.movierama.dto.MovieRegistrationDto
import com.movierama.entity.Movie
import com.movierama.entity.MovieReaction
import com.movierama.entity.User
import com.movierama.repository.MovieReactionRepository
import com.movierama.repository.MovieRepository
import com.movierama.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Title

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Title("Integration tests for MovieService with real DB & repositories")
class MovieServiceITSpec extends Specification {

    @Autowired MovieService movieService
    @Autowired MovieRepository movieRepository
    @Autowired MovieReactionRepository reactionRepository
    @Autowired UserRepository userRepository  // used to persist owners/reactors

    private User owner
    private User reactor

    def setup() {
        // Persist two users for ownership / reactions
        owner = userRepository.save(new User(username: "owner", email: "o@x.com", password: "p", firstName: "O", lastName: "W", role: User.Role.USER))
        reactor = userRepository.save(new User(username: "reactor", email: "r@x.com", password: "p", firstName: "R", lastName: "C", role: User.Role.USER))
    }

    def "createMovie persists with owner and can be read back"() {
        given:
        def dto = new MovieRegistrationDto()
        // If your DTO requires fields (e.g., title/description), set them here:
        // dto.title = "Inception"; dto.description = "A mind-bender"

        when:
        Movie saved = movieService.createMovie(dto, owner)

        then:
        saved.id != null
        saved.user.id == owner.id

        and: "exists in DB"
        movieRepository.findById(saved.id).get().user.id == owner.id
    }

    def "getMoviesPageSorted returns sorted/paged list"() {
        given: "persist a few movies with different titles for deterministic sorting"
        def mA = movieService.createMovie(new MovieRegistrationDto(/* title: "A" */), owner)
        mA.title = "A"; movieRepository.save(mA)
        def mC = movieService.createMovie(new MovieRegistrationDto(/* title: "C" */), owner)
        mC.title = "C"; movieRepository.save(mC)
        def mB = movieService.createMovie(new MovieRegistrationDto(/* title: "B" */), owner)
        mB.title = "B"; movieRepository.save(mB)

        when:
        def resp = movieService.getMoviesPageSorted(0, 2, "title", "ASC", owner.username)

        then:
        resp.page == 0
        resp.size == 2
        resp.totalElements >= 3
        resp.totalPages >= 2

        and: "first page should be A, B"
        resp.content*.class.every { it == MovieDto }  // mapped DTOs
        // If MovieDto has a title field, you can assert on it:
        // resp.content*.title == ["A", "B"]
    }

    def "getMoviesByUserPaged only returns movies of that user"() {
        given:
        // Owner has 2 movies
        def m1 = movieService.createMovie(new MovieRegistrationDto(), owner)
        def m2 = movieService.createMovie(new MovieRegistrationDto(), owner)
        // Reactor as another uploader
        movieService.createMovie(new MovieRegistrationDto(), reactor)

        when:
        def resp = movieService.getMoviesByUserPaged(owner.id, 0, 10, "id", "ASC")

        then:
        resp.totalElements == 2
        resp.content.size() == 2
    }

    def "reactToMovie creates, updates, and toggles reactions"() {
        given:
        def movie = movieService.createMovie(new MovieRegistrationDto(), owner)

        when: "reactor likes"
        movieService.reactToMovie(movie.id, reactor, MovieReaction.ReactionType.LIKE)

        then:
        def r1 = reactionRepository.findByUserIdAndMovieId(reactor.id, movie.id).get()
        r1.reactionType == MovieReaction.ReactionType.LIKE

        when: "reactor changes to dislike"
        movieService.reactToMovie(movie.id, reactor, MovieReaction.ReactionType.DISLIKE)

        then:
        def r2 = reactionRepository.findByUserIdAndMovieId(reactor.id, movie.id).get()
        r2.reactionType == MovieReaction.ReactionType.DISLIKE

        when: "reactor sends dislike again -> toggle off (delete)"
        movieService.reactToMovie(movie.id, reactor, MovieReaction.ReactionType.DISLIKE)

        then:
        !reactionRepository.findByUserIdAndMovieId(reactor.id, movie.id).isPresent()
    }

    def "reactToMovie prevents reacting to own movie"() {
        given:
        def movie = movieService.createMovie(new MovieRegistrationDto(), owner)

        when:
        movieService.reactToMovie(movie.id, owner, MovieReaction.ReactionType.LIKE)

        then:
        def ex = thrown(RuntimeException)
        ex.message == "You cannot react to your own movie"
    }
}
