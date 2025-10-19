package com.movierama.repository;

import com.movierama.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<Movie> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Movie> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT m FROM Movie m WHERE m.title ILIKE %:title% OR m.description ILIKE %:description%")
    List<Movie> findByTitleOrDescriptionContainingIgnoreCase(@Param("title") String title, @Param("description") String description);
    
    @Query("SELECT m FROM Movie m LEFT JOIN m.reactions r " +
           "GROUP BY m.id " +
           "ORDER BY COUNT(CASE WHEN r.reactionType = 'LIKE' THEN 1 END) DESC, m.createdAt DESC")
    List<Movie> findAllOrderByLikesDesc();
    
    @Query("SELECT m FROM Movie m LEFT JOIN m.reactions r " +
           "GROUP BY m.id " +
           "ORDER BY COUNT(CASE WHEN r.reactionType = 'HATE' THEN 1 END) DESC, m.createdAt DESC")
    List<Movie> findAllOrderByHatesDesc();

    Optional<Movie> findByTitleIgnoreCase(String title);
}
