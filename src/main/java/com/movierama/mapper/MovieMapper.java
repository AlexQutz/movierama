package com.movierama.mapper;

import com.movierama.dto.MovieDto;
import com.movierama.dto.MovieRegistrationDto;
import com.movierama.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    MovieMapper INSTANCE = Mappers.getMapper(MovieMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    Movie toEntity(MovieRegistrationDto dto);

    @Mapping(target = "userName", expression = "java(movie.getUserName())")
    @Mapping(target = "likeCount", expression = "java(movie.getLikeCount())")
    @Mapping(target = "hateCount", expression = "java(movie.getHateCount())")
    MovieDto toDto(Movie movie);
}

