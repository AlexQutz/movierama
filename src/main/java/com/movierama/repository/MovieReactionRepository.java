package com.movierama.repository;

import com.movierama.entity.MovieReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieReactionRepository extends JpaRepository<MovieReaction, Long> {
    
    Optional<MovieReaction> findByUserIdAndMovieId(Long userId, Long movieId);
}







