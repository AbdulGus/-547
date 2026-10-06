package ru.library.dto;

import jakarta.validation.constraints.*;

public record AuthorRequest(@NotBlank @Size(max = 120) String name) {}
