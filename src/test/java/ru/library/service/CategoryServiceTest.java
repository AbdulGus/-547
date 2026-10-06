package ru.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.library.dto.CategoryRequest;
import ru.library.entity.Category;
import ru.library.exception.ApiException;
import ru.library.repository.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repository;
    @Mock BookRepository books;
    @InjectMocks CategoryService service;

    @Test
    void cannotDeleteCategoryWithBooks() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Category()));
        when(books.existsByCategoriesId(1L)).thenReturn(true);
        ApiException error = assertThrows(ApiException.class, () -> service.delete(1L));
        assertEquals(409, error.getStatus().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void rejectsDuplicateCategoryIgnoringCaseAndSpaces() {
        when(repository.existsByNameIgnoreCaseAndIdNot("классика", 0L)).thenReturn(true);
        ApiException error = assertThrows(ApiException.class,
            () -> service.create(new CategoryRequest("  классика  ")));
        assertEquals(409, error.getStatus().value());
        verify(repository, never()).save(any());
    }

    @Test
    void updateExcludesCurrentCategoryFromUniquenessCheck() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Роман");
        when(repository.findById(1L)).thenReturn(Optional.of(category));
        when(repository.save(category)).thenReturn(category);
        assertEquals("РОМАН", service.update(1L, new CategoryRequest(" РОМАН ")).name());
        verify(repository).existsByNameIgnoreCaseAndIdNot("РОМАН", 1L);
    }
}
