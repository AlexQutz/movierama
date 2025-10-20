package com.movierama.specification;

import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public final class MovieSpecification {

    private MovieSpecification() {}

    public static Specification<Movie> orderByReaction(String sortBy, String sortDirection) {
        return (root, query, cb) -> {
            boolean asc = "asc".equalsIgnoreCase(sortDirection);

            assert query != null;
            Subquery<Long> likeCountSq = query.subquery(Long.class);
            Root<MovieReaction> r1 = likeCountSq.from(MovieReaction.class);
            likeCountSq.select(cb.count(r1));
            likeCountSq.where(
                    cb.equal(r1.get("movie"), root),
                    cb.equal(r1.get("reactionType"), MovieReaction.ReactionType.LIKE)
            );

            Subquery<Long> hateCountSq = query.subquery(Long.class);
            Root<MovieReaction> r2 = hateCountSq.from(MovieReaction.class);
            hateCountSq.select(cb.count(r2));
            hateCountSq.where(
                    cb.equal(r2.get("movie"), root),
                    cb.equal(r2.get("reactionType"), MovieReaction.ReactionType.HATE)
            );

            if ("likeCount".equalsIgnoreCase(sortBy)) {
                query.orderBy(asc ? cb.asc(likeCountSq) : cb.desc(likeCountSq),
                        cb.asc(root.get("id"))); // stable tiebreaker
            } else if ("hateCount".equalsIgnoreCase(sortBy)) {
                query.orderBy(asc ? cb.asc(hateCountSq) : cb.desc(hateCountSq),
                        cb.asc(root.get("id")));
            }
            return cb.conjunction();
        };
    }
}
