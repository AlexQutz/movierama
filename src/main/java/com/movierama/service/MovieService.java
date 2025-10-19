package com.movierama.service;

import com.movierama.dto.MovieDto;
import com.movierama.dto.MovieRegistrationDto;
import com.movierama.mapper.MovieMapper;
import com.movierama.paging.PagingResponse;
import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import com.movierama.entity.User;
import com.movierama.repository.MovieReactionRepository;
import com.movierama.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieReactionRepository reactionRepository;
    private final MovieMapper movieMapper;

    @Transactional
    public Movie createMovie(MovieRegistrationDto movieDto, User user) {
        Movie movie = movieMapper.toEntity(movieDto);
        movie.setUser(user);
        return movieRepository.save(movie);
    }

    @Transactional(readOnly = true)
    public PagingResponse<MovieDto> getMoviesPageSorted(
            int page, int size, String sortBy, String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<Movie> moviePage = movieRepository.findAll(pageable);

        List<MovieDto> dtos = moviePage.getContent()
                .stream()
                .map(movieMapper::toDto)
                .toList();

        PagingResponse<MovieDto> response = new PagingResponse<>();
        response.setContent(dtos);
        response.setPage(moviePage.getNumber());
        response.setSize(moviePage.getSize());
        response.setTotalElements(moviePage.getTotalElements());
        response.setTotalPages(moviePage.getTotalPages());
        response.setLast(moviePage.isLast());
        return response;
    }

    @Transactional(readOnly = true)
    public PagingResponse<MovieDto> getMoviesByUserPaged(
            Long userId, int page, int size, String sortBy, String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Movie> moviePage = movieRepository.findByUserId(userId, pageable);

        List<MovieDto> dtos = moviePage.getContent()
                .stream()
                .map(movieMapper::toDto)
                .toList();

        PagingResponse<MovieDto> response = new PagingResponse<>();
        response.setContent(dtos);
        response.setPage(moviePage.getNumber());
        response.setSize(moviePage.getSize());
        response.setTotalElements(moviePage.getTotalElements());
        response.setTotalPages(moviePage.getTotalPages());
        response.setLast(moviePage.isLast());

        return response;
    }

    @Transactional
    public void reactToMovie(Long movieId, User userProfile, MovieReaction.ReactionType reactionType) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        if (movie.getUser().getId().equals(userProfile.getId())) {
            throw new RuntimeException("You cannot react to your own movie");
        }

        MovieReaction existingReaction = reactionRepository.findByUserIdAndMovieId(userProfile.getId(), movieId)
                .orElse(null);

        if (existingReaction != null) {
            if (existingReaction.getReactionType() == reactionType) {
                reactionRepository.delete(existingReaction);
            } else {
                existingReaction.setReactionType(reactionType);
                reactionRepository.save(existingReaction);
            }
        } else {
            MovieReaction reaction = new MovieReaction();
            reaction.setMovie(movie);
            reaction.setUser(userProfile);
            reaction.setReactionType(reactionType);
            reactionRepository.save(reaction);
        }
    }
}
