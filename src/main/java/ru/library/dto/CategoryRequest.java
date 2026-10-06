package ru.library.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(@NotBlank @Size(max = 80) String name) {}
