package ru.library.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record BookRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Pattern(regexp = "\\d{13}", message = "ISBN должен содержать 13 цифр") String isbn,
    @NotNull @Min(1450) Integer publicationYear,
    @NotNull @Positive Long authorId,
    @NotEmpty @Size(max = 10) Set<@NotNull @Positive Long> categoryIds
) {}
