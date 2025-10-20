package com.movierama.repository;

import com.movierama.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    Page<Movie> findByUserId(Long userId, Pageable pageable);

    Optional<Movie> findByTitleIgnoreCase(String title);

    Page<Movie> findAll(Specification<Movie> movieSpecification, Pageable pageable);
}
