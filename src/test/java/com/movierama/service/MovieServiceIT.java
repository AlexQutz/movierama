package com.movierama.service;

import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import com.movierama.entity.User;
import com.movierama.repository.MovieReactionRepository;
import com.movierama.repository.MovieRepository;
import com.movierama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class MovieServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private final MovieService movieService;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MovieReactionRepository movieReactionRepository;

    private User owner;
    private User voter;
    private Movie movie;

    @BeforeEach
    void setup() {
        movieReactionRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@example.com");
        owner.setPassword("pw");
        owner.setFirstName("Own");
        owner.setLastName("Er");
        owner = userRepository.save(owner);

        voter = new User();
        voter.setUsername("voter");
        voter.setEmail("voter@example.com");
        voter.setPassword("pw");
        voter.setFirstName("Vo");
        voter.setLastName("Ter");
        voter = userRepository.save(voter);

        movie = new Movie();
        movie.setTitle("Inception");
        movie.setDescription("Mind-bender");
        movie.setUser(owner);
        movie = movieRepository.save(movie);
    }

    @Test
    @DisplayName("User can like then toggle off their reaction")
    void react_toggle_flow() {
        movieService.reactToMovie(movie.getId(), voter, MovieReaction.ReactionType.LIKE);
        assertThat(movieReactionRepository.findByUserIdAndMovieId(voter.getId(), movie.getId())).isPresent();

        movieService.reactToMovie(movie.getId(), voter, MovieReaction.ReactionType.LIKE);
        assertThat(movieReactionRepository.findByUserIdAndMovieId(voter.getId(), movie.getId())).isNotPresent();
    }

    @Test
    @DisplayName("Switching reaction updates persisted value")
    void react_switch_flow() {
        movieService.reactToMovie(movie.getId(), voter, MovieReaction.ReactionType.LIKE);
        movieService.reactToMovie(movie.getId(), voter, MovieReaction.ReactionType.HATE);

        var r = movieReactionRepository.findByUserIdAndMovieId(voter.getId(), movie.getId());
        assertThat(r).isPresent();
        assertThat(r.get().getReactionType()).isEqualTo(MovieReaction.ReactionType.HATE);
    }
}
