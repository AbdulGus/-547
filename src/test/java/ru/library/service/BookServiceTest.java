package ru.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.library.dto.BookRequest;
import ru.library.entity.Author;
import ru.library.exception.ApiException;
import ru.library.repository.*;
import java.time.Year;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock BookRepository books;
    @Mock AuthorRepository authors;
    @Mock CategoryRepository categories;
    @InjectMocks BookService service;

    @Test
    void rejectsFuturePublicationYear() {
        var request = new BookRequest("Книга", "9785170906307", Year.now().getValue() + 1, 1L, Set.of(1L));
        ApiException error = assertThrows(ApiException.class, () -> service.create(request));
        assertEquals(400, error.getStatus().value());
        verifyNoInteractions(books, authors, categories);
    }

    @Test
    void rejectsDuplicateIsbn() {
        when(books.existsByIsbnAndIdNot("9785170906307", 0L)).thenReturn(true);
        ApiException error = assertThrows(ApiException.class,
            () -> service.create(new BookRequest("Книга", "9785170906307", 2020, 1L, Set.of(1L))));
        assertEquals(409, error.getStatus().value());
        verify(books, never()).save(any());
    }

    @Test
    void rejectsMissingCategory() {
        when(authors.findById(1L)).thenReturn(Optional.of(new Author()));
        when(categories.findAllById(Set.of(15L))).thenReturn(List.of());
        ApiException error = assertThrows(ApiException.class,
            () -> service.create(new BookRequest("Книга", "9785170906307", 2020, 1L, Set.of(15L))));
        assertEquals(404, error.getStatus().value());
        verify(books, never()).save(any());
    }
}
