package ru.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.library.entity.Author;
import ru.library.exception.ApiException;
import ru.library.repository.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {
    @Mock AuthorRepository repository;
    @Mock BookRepository books;
    @InjectMocks AuthorService service;

    @Test
    void cannotDeleteAuthorWithBooks() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Author()));
        when(books.existsByAuthorId(1L)).thenReturn(true);
        ApiException error = assertThrows(ApiException.class, () -> service.delete(1L));
        assertEquals(409, error.getStatus().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesUnusedAuthor() {
        Author author = new Author();
        when(repository.findById(1L)).thenReturn(Optional.of(author));
        service.delete(1L);
        verify(repository).delete(author);
    }
}
