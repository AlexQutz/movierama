package com.movierama.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MovieDto {

    private Long id;

    private String title;

    private String description;

    private String userName;

    private Long userId;

    private LocalDateTime createdAt;

    private long likeCount;

    private long hateCount;

    private boolean userLiked;

    private boolean userHated;
}







