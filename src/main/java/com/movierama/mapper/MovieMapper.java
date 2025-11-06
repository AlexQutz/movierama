package com.movierama.mapper;

import com.movierama.dto.MovieDto;
import com.movierama.dto.MovieRegistrationDto;
import com.movierama.entity.Movie;
import com.movierama.entity.MovieReaction;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {MovieMapper.class}
)
public interface MovieMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    Movie toEntity(MovieRegistrationDto dto);

    // Base mapping without user context
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", expression = "java(movie.getUserName())")
    @Mapping(target = "likeCount", expression = "java(movie.getLikeCount())")
    @Mapping(target = "hateCount", expression = "java(movie.getHateCount())")
    @Mapping(target = "userLiked", ignore = true)
    @Mapping(target = "userHated", ignore = true)
    MovieDto toDto(Movie movie);

    // Overload with User context
    default MovieDto toDto(Movie movie, Long currentUserId) {
        if (movie == null) {
            return null;
        }

        MovieDto dto = toDto(movie);

        if (currentUserId == null) {
            dto.setUserLiked(false);
            dto.setUserHated(false);
            return dto;
        }

        boolean liked = movie.getReactions() != null &&
                movie.getReactions().stream()
                        .anyMatch(r -> r.getUser() != null &&
                                r.getUser().getId().equals(currentUserId) &&
                                r.getReactionType() == MovieReaction.ReactionType.LIKE);

        boolean hated = movie.getReactions() != null &&
                movie.getReactions().stream()
                        .anyMatch(r -> r.getUser() != null &&
                                r.getUser().getId().equals(currentUserId) &&
                                r.getReactionType() == MovieReaction.ReactionType.HATE);

        dto.setUserLiked(liked);
        dto.setUserHated(hated);

        return dto;
    }
}
