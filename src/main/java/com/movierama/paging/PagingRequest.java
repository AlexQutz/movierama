package com.movierama.paging;

import lombok.Data;

@Data
public class PagingRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String sortDirection = "DESC";
}
