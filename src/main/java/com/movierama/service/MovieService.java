package com.movierama.service;

import com.movierama.dto.MovieDto;
import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import com.movierama.entity.User;
import com.movierama.repository.MovieReactionRepository;
import com.movierama.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final MovieReactionRepository movieReactionRepository;
    
    @Transactional(readOnly = true)
    public List<MovieDto> getAllMovies() {
        return movieRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::convertToDto)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MovieDto> getAllMovies(User currentUser) {
        List<Movie> movies = movieRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).getContent();
        return movies.stream()
                .map(movie -> convertToDtoWithUserReaction(movie, currentUser))
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Page<MovieDto> getMoviesPage(Pageable pageable) {
        return movieRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::convertToDto);
    }
    
    @Transactional(readOnly = true)
    public Page<MovieDto> getMoviesPage(Pageable pageable, User currentUser) {
        return movieRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(movie -> convertToDtoWithUserReaction(movie, currentUser));
    }

    @Transactional(readOnly = true)
    public Page<MovieDto> getMoviesPageSorted(String sort, Pageable pageable, User currentUser) {
        String normalized = sort == null ? "date" : sort.trim().toLowerCase();
        return switch (normalized) {
            case "likes" -> {
                List<Movie> ordered = movieRepository.findAllOrderByLikesDesc();
                yield toPage(ordered, pageable, currentUser);
            }
            case "hates" -> {
                List<Movie> ordered = movieRepository.findAllOrderByHatesDesc();
                yield toPage(ordered, pageable, currentUser);
            }
            default -> getMoviesPage(pageable, currentUser);
        };
    }

    private Page<MovieDto> toPage(List<Movie> ordered, Pageable pageable, User currentUser) {
        int total = ordered.size();
        int from = Math.min((int) pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        List<MovieDto> content = ordered.subList(from, to).stream()
                .map(m -> currentUser != null ? convertToDtoWithUserReaction(m, currentUser) : convertToDto(m))
                .toList();
        return new PageImpl<>(content, pageable, total);
    }
    
    @Transactional(readOnly = true)
    public List<MovieDto> getMoviesByUser(Long userId) {
        return movieRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }
    
    public Movie createMovie(Movie movie, User user) {
        movie.setUser(user);
        movieRepository.findByUserIdAndTitleIgnoreCase(user.getId(), movie.getTitle())
                .ifPresent(existing -> { throw new RuntimeException("You already added a movie with this title"); });
        try {
            return movieRepository.saveAndFlush(movie);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Duplicate movie title for this user", ex);
        }
    }
    
    public void reactToMovie(Long movieId, User user, MovieReaction.ReactionType reactionType) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (movie.getUser() != null && Objects.equals(movie.getUser().getId(), user.getId())) {
            throw new RuntimeException("You cannot vote for your own movie");
        }

        MovieReaction existingReaction = movieReactionRepository.findByUserIdAndMovieId(user.getId(), movieId)
                .orElse(null);
        
        if (existingReaction != null) {
            if (existingReaction.getReactionType() == reactionType) {
                movieReactionRepository.delete(existingReaction);
            } else {
                existingReaction.setReactionType(reactionType);
                movieReactionRepository.save(existingReaction);
            }
        } else {
            MovieReaction newReaction = new MovieReaction();
            newReaction.setMovie(movie);
            newReaction.setUser(user);
            newReaction.setReactionType(reactionType);
            movieReactionRepository.save(newReaction);
        }
    }
    
    private MovieDto convertToDto(Movie movie) {
        return new MovieDto(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getUserName(),
                movie.getCreatedAt(),
                movie.getLikeCount(),
                movie.getHateCount()
        );
    }
    
    private MovieDto convertToDtoWithUserReaction(Movie movie, User currentUser) {
        MovieDto dto = convertToDto(movie);
        
        if (currentUser != null) {
            MovieReaction userReaction = movieReactionRepository.findByUserIdAndMovieId(currentUser.getId(), movie.getId())
                    .orElse(null);
            
            if (userReaction != null) {
                dto.setUserLiked(userReaction.getReactionType() == MovieReaction.ReactionType.LIKE);
                dto.setUserHated(userReaction.getReactionType() == MovieReaction.ReactionType.HATE);
            }
        }
        
        return dto;
    }
}
