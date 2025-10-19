package com.movierama.paging;

import lombok.Data;

import java.util.List;

@Data
public class PagingResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
