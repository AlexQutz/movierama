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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;


@Tag(name = "Movies", description = "Movie management endpoints")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final MovieMapper movieMapper;
    private final MovieRepository movieRepository;

    @Operation(summary = "List all movies", description = "Returns a paginated list of all movies with optional sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagingResponse.class)))
    })
    @PostMapping("/movies")
    public ResponseEntity<PagingResponse<MovieDto>> listMovies(
            @RequestBody PagingRequest pagingRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        PagingResponse<MovieDto> response = movieService.getMoviesPageSorted(
                pagingRequest.getPage(),
                pagingRequest.getSize(),
                !Objects.equals(pagingRequest.getSortBy(), "") ? pagingRequest.getSortBy() : "",
                pagingRequest.getSortDirection() != null ? pagingRequest.getSortDirection() : "",
                user
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List movies by user", description = "Returns a paginated list of movies posted by a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagingResponse.class)))
    })
    @PostMapping("/movies/user/{userId}")
    public ResponseEntity<PagingResponse<MovieDto>> listMoviesByUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @RequestBody PagingRequest pagingRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        if (pagingRequest == null) {
            pagingRequest = new PagingRequest();
        }

        PagingResponse<MovieDto> response = movieService.getMoviesByUserPaged(
                userId,
                pagingRequest.getPage(),
                pagingRequest.getSize(),
                pagingRequest.getSortBy(),
                pagingRequest.getSortDirection(),
                user
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new movie", description = "Creates a new movie post. Requires authentication.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movie created successfully",
                    content = @Content(schema = @Schema(implementation = MovieDto.class))),
            @ApiResponse(responseCode = "400", description = "Movie with same title already exists or invalid input",
                    content = @Content(schema = @Schema(implementation = com.movierama.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = com.movierama.exception.ErrorResponse.class)))
    })
    @PostMapping("/secured/movies")
    public ResponseEntity<MovieDto> createMovie(
            @RequestBody MovieRegistrationDto movie,
            @Parameter(hidden = true) @AuthenticationPrincipal User userProfile
    ) {

        String title = movie.getTitle().trim();

        if (movieRepository.findByTitleIgnoreCase(title).isPresent()) {
            throw new RuntimeException("A movie with the same title already exists");
        }

        Movie savedMovie = movieService.createMovie(movie, userProfile);
        MovieDto movieDto = movieMapper.toDto(savedMovie);
        return ResponseEntity.ok(movieDto);
    }




    @Operation(summary = "React to a movie", description = "Like or dislike a movie. Requires authentication. Cannot react to your own movies.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reaction updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reaction type, movie not found, or cannot react to own movie",
                    content = @Content(schema = @Schema(implementation = com.movierama.exception.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = com.movierama.exception.ErrorResponse.class)))
    })
    @PostMapping("/secured/movies/{movieId}/react")
    public ResponseEntity<Map<String, String>> reactToMovie(
            @Parameter(description = "Movie ID", required = true) @PathVariable Long movieId,
            @Parameter(description = "Reaction type: LIKE or DISLIKE (HATE)", required = true) @RequestParam String reaction,
            @Parameter(hidden = true) @AuthenticationPrincipal User userProfile
    ) {
        if (userProfile == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication required"));
        }

        MovieReaction.ReactionType reactionType =
                MovieReaction.ReactionType.valueOf(reaction.toUpperCase());
        movieService.reactToMovie(movieId, userProfile, reactionType);
        return ResponseEntity.ok(Map.of("message", "Reaction updated successfully"));
    }

}
