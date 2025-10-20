package com.movierama.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieRegistrationDto {

    @NotNull
    private String title;

    @NotNull
    private String description;

}

