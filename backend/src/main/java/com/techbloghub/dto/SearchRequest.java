package com.techbloghub.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {
    private String query;
    private List<String> companies;
    private List<String> tags;
    private List<String> categories;
    private String sortBy = "publishedAt";
    private String sortDirection = "desc";
    private int page = 0;
    private int size = 20;
}