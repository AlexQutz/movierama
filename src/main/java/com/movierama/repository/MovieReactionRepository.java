package com.movierama.repository;

import com.movierama.entity.MovieReaction;
import com.movierama.entity.MovieReaction.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieReactionRepository extends JpaRepository<MovieReaction, Long> {
    
    Optional<MovieReaction> findByUserIdAndMovieId(Long userId, Long movieId);
    
    List<MovieReaction> findByMovieIdAndReactionType(Long movieId, ReactionType reactionType);
    
    long countByMovieIdAndReactionType(Long movieId, ReactionType reactionType);
    
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    
    @Query("SELECT COUNT(r) FROM MovieReaction r WHERE r.movie.id = :movieId AND r.reactionType = :reactionType")
    long countReactionsByMovieIdAndType(@Param("movieId") Long movieId, @Param("reactionType") ReactionType reactionType);
    
    @Query("SELECT r FROM MovieReaction r WHERE r.user.id = :userId")
    List<MovieReaction> findByUserId(@Param("userId") Long userId);
}





