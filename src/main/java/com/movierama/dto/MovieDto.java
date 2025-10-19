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
    private LocalDateTime createdAt;
    private long likeCount;
    private long hateCount;
    private boolean userLiked;
    private boolean userHated;


    public MovieDto(Long id, String title, String description, String userName,
                   LocalDateTime createdAt, long likeCount, long hateCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.userName = userName;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.hateCount = hateCount;
    }


}







