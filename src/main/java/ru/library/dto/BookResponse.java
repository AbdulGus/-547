package ru.library.dto;

import java.util.List;

public record BookResponse(Long id, String title, String isbn, Integer publicationYear,
                           NamedResponse author, List<NamedResponse> categories) {}
