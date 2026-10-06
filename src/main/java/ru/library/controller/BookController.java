package ru.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.library.dto.*;
import ru.library.service.BookService;
import java.net.URI;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Книги")
public class BookController {
    private final BookService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Список: книги", description = "Страницы нумеруются с нуля. Размер страницы от 1 до 100.")
    public PageResponse<BookResponse> list(@RequestParam(defaultValue = "") @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.list(search, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить запись по ID")
    public BookResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать запись", description = "Только ADMIN")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/books/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить запись", description = "Только ADMIN")
    public BookResponse update(@PathVariable @Positive Long id, @Valid @RequestBody BookRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить запись", description = "Только ADMIN")
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}
