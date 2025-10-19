package com.movierama.controller;

import com.movierama.dto.MovieDto;
import com.movierama.dto.MovieRegistrationDto;
import com.movierama.mapper.MovieMapper;
import com.movierama.paging.PagingRequest;
import com.movierama.paging.PagingResponse;
import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import com.movierama.entity.User;
import com.movierama.repository.MovieRepository;
import com.movierama.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;
    private final MovieRepository movieRepository;

    @PostMapping("/movies")
    public ResponseEntity<PagingResponse<MovieDto>> listMovies(
            @RequestBody PagingRequest pagingRequest
    ) {
        PagingResponse<MovieDto> response = movieService.getMoviesPageSorted(
                pagingRequest.getPage(),
                pagingRequest.getSize(),
                pagingRequest.getSortBy(),
                pagingRequest.getSortDirection()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/secured/movies")
    public ResponseEntity<MovieDto> createMovie(
            @RequestBody MovieRegistrationDto movie,
            @AuthenticationPrincipal User userProfile
    ) {

        String title = movie.getTitle().trim();

        if (movieRepository.findByTitleIgnoreCase(title).isPresent()) {
            throw new RuntimeException("A movie with the same title already exists");
        }

        Movie savedMovie = movieService.createMovie(movie, userProfile);
        MovieDto movieDto = movieMapper.toDto(savedMovie);
        return ResponseEntity.ok(movieDto);
    }


    @PostMapping("secured/movies/user/{userId}")
    public ResponseEntity<PagingResponse<MovieDto>> listByUser(
            @PathVariable Long userId,
            @RequestBody(required = false) PagingRequest pagingRequest
    ) {
        if (pagingRequest == null) {
            pagingRequest = new PagingRequest();
        }

        PagingResponse<MovieDto> response = movieService.getMoviesByUserPaged(
                userId,
                pagingRequest.getPage(),
                pagingRequest.getSize(),
                pagingRequest.getSortBy(),
                pagingRequest.getSortDirection()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/secured/movies/{movieId}/react")
    public ResponseEntity<String> reactToMovie(
            @PathVariable Long movieId,
            @RequestParam String reaction,
            @AuthenticationPrincipal User userProfile
    ) {
        if (userProfile == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        try {
            MovieReaction.ReactionType reactionType = MovieReaction.ReactionType.valueOf(reaction.toUpperCase());
            movieService.reactToMovie(movieId, userProfile, reactionType);
            return ResponseEntity.ok("Reaction updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid reaction type");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
