package ru.library.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "[\\x21-\\x7E]+", message = "Используйте латинские буквы, цифры и символы без пробелов") String password
) {}
