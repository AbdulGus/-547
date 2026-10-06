package ru.library.dto;

public record AuthResponse(String token, String tokenType, long expiresIn, String email, String role) {}
