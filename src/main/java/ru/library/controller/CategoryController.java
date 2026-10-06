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
import ru.library.service.CategoryService;
import java.net.URI;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Категории")
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Список: категории", description = "Страницы нумеруются с нуля. Размер страницы от 1 до 100.")
    public PageResponse<NamedResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить запись по ID")
    public NamedResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать запись", description = "Только ADMIN")
    public ResponseEntity<NamedResponse> create(@Valid @RequestBody CategoryRequest request) {
        NamedResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/categories/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить запись", description = "Только ADMIN")
    public NamedResponse update(@PathVariable @Positive Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить запись", description = "Только ADMIN. Нельзя удалить запись, пока с ней связаны книги.")
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}
