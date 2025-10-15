package com.movierama.service;

import com.movierama.dto.MovieDto;
import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import com.movierama.entity.User;
import com.movierama.repository.MovieReactionRepository;
import com.movierama.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieReactionRepository movieReactionRepository;

    @InjectMocks
    private MovieService movieService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUsername("jdoe");
        user.setEmail("j@d.com");
        user.setPassword("pw");
    }

    private Movie buildMovie(long id, String title, User owner) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle(title);
        m.setDescription("desc");
        m.setCreatedAt(LocalDateTime.now());
        m.setUser(owner);
        m.setReactions(new ArrayList<>());
        return m;
    }

    @Test
    @DisplayName("getAllMovies returns DTOs ordered by createdAt desc")
    void getAllMovies_returnsDtos() {
        List<Movie> movies = List.of(buildMovie(1, "A", user), buildMovie(2, "B", user));
        when(movieRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(movies));

        List<MovieDto> result = movieService.getAllMovies();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(movieRepository).findAllByOrderByCreatedAtDesc(Pageable.unpaged());
    }

    @Test
    @DisplayName("getMoviesPage maps to DTOs and preserves pagination")
    void getMoviesPage_mapsAndPaginates() {
        Pageable pageable = PageRequest.of(0, 2);
        List<Movie> movies = List.of(buildMovie(1, "A", user), buildMovie(2, "B", user));
        when(movieRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(movies, pageable, 4));

        Page<MovieDto> page = movieService.getMoviesPage(pageable);

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).extracting(MovieDto::getTitle).containsExactly("A", "B");
    }

    @Test
    @DisplayName("createMovie saves when no duplicate for user")
    void createMovie_saves() {
        Movie newMovie = buildMovie(0, "Title", user);
        when(movieRepository.findByUserIdAndTitleIgnoreCase(user.getId(), newMovie.getTitle()))
                .thenReturn(Optional.empty());
        when(movieRepository.saveAndFlush(any(Movie.class))).thenAnswer(inv -> {
            Movie m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        Movie saved = movieService.createMovie(newMovie, user);

        assertThat(saved.getId()).isEqualTo(10L);
        verify(movieRepository).saveAndFlush(any(Movie.class));
    }

    @Test
    @DisplayName("createMovie throws on duplicate title per user")
    void createMovie_duplicate_throws() {
        Movie newMovie = buildMovie(0, "Title", user);
        when(movieRepository.findByUserIdAndTitleIgnoreCase(user.getId(), "Title"))
                .thenReturn(Optional.of(buildMovie(99, "Title", user)));

        assertThatThrownBy(() -> movieService.createMovie(newMovie, user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already added a movie");
        verify(movieRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reactToMovie toggles off same reaction")
    void reactToMovie_toggleOff() {
        User voter = new User();
        voter.setId(2L);
        Movie movie = buildMovie(5, "M", user);
        MovieReaction existing = new MovieReaction();
        existing.setId(7L);
        existing.setMovie(movie);
        existing.setUser(voter);
        existing.setReactionType(MovieReaction.ReactionType.LIKE);

        when(movieRepository.findById(5L)).thenReturn(Optional.of(movie));
        when(movieReactionRepository.findByUserIdAndMovieId(2L, 5L)).thenReturn(Optional.of(existing));

        movieService.reactToMovie(5L, voter, MovieReaction.ReactionType.LIKE);

        verify(movieReactionRepository).delete(existing);
        verify(movieReactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("reactToMovie switches reaction when different")
    void reactToMovie_switches() {
        User voter = new User();
        voter.setId(2L);
        Movie movie = buildMovie(5, "M", user);
        MovieReaction existing = new MovieReaction();
        existing.setId(7L);
        existing.setMovie(movie);
        existing.setUser(voter);
        existing.setReactionType(MovieReaction.ReactionType.LIKE);

        when(movieRepository.findById(5L)).thenReturn(Optional.of(movie));
        when(movieReactionRepository.findByUserIdAndMovieId(2L, 5L)).thenReturn(Optional.of(existing));

        movieService.reactToMovie(5L, voter, MovieReaction.ReactionType.HATE);

        ArgumentCaptor<MovieReaction> captor = ArgumentCaptor.forClass(MovieReaction.class);
        verify(movieReactionRepository).save(captor.capture());
        assertThat(captor.getValue().getReactionType()).isEqualTo(MovieReaction.ReactionType.HATE);
    }

    @Test
    @DisplayName("reactToMovie creates new reaction when none exists")
    void reactToMovie_createsNew() {
        User voter = new User();
        voter.setId(2L);
        Movie movie = buildMovie(5, "M", user);

        when(movieRepository.findById(5L)).thenReturn(Optional.of(movie));
        when(movieReactionRepository.findByUserIdAndMovieId(2L, 5L)).thenReturn(Optional.empty());

        movieService.reactToMovie(5L, voter, MovieReaction.ReactionType.LIKE);

        verify(movieReactionRepository).save(any(MovieReaction.class));
    }

    @Test
    @DisplayName("reactToMovie prevents self-vote")
    void reactToMovie_selfVote_throws() {
        User owner = new User();
        owner.setId(3L);
        Movie movie = buildMovie(5, "M", owner);
        when(movieRepository.findById(5L)).thenReturn(Optional.of(movie));

        assertThatThrownBy(() -> movieService.reactToMovie(5L, owner, MovieReaction.ReactionType.LIKE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot vote for your own movie");
        verify(movieReactionRepository, never()).save(any());
    }
}
