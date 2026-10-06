package ru.library.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.dto.*;
import ru.library.entity.*;
import ru.library.exception.ApiException;
import ru.library.repository.*;
import java.time.Year;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository books;
    private final AuthorRepository authors;
    private final CategoryRepository categories;

    public PageResponse<BookResponse> list(String search, int page, int size) {
        Page<Long> ids = books.findPageIds(search, PageRequest.of(page, size, Sort.by("id").descending()));
        Map<Long, Book> fetched = ids.isEmpty() ? Map.of() : books.findWithRelationsByIdIn(ids.getContent())
            .stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        return PageResponse.from(ids.map(id -> toResponse(fetched.get(id))));
    }

    public BookResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = new Book();
        apply(book, request);
        return toResponse(books.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = find(id);
        apply(book, request);
        return toResponse(books.save(book));
    }

    @Transactional
    public void delete(Long id) {
        books.delete(find(id));
    }

    private Book find(Long id) {
        return books.findWithRelationsById(id).orElseThrow(() -> ApiException.notFound("Книга"));
    }

    private void apply(Book book, BookRequest request) {
        if (request.publicationYear() > Year.now().getValue()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Год издания не может быть в будущем");
        }
        if (books.existsByIsbnAndIdNot(request.isbn(), book.getId() == null ? 0L : book.getId())) {
            throw ApiException.conflict("Книга с таким ISBN уже существует");
        }
        Author author = authors.findById(request.authorId()).orElseThrow(() -> ApiException.notFound("Автор"));
        List<Category> selected = categories.findAllById(request.categoryIds());
        if (selected.size() != request.categoryIds().size()) {
            throw ApiException.notFound("Категория");
        }
        book.setTitle(request.title().trim());
        book.setIsbn(request.isbn());
        book.setPublicationYear(request.publicationYear());
        book.setAuthor(author);
        book.setCategories(new HashSet<>(selected));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getIsbn(), book.getPublicationYear(),
            new NamedResponse(book.getAuthor().getId(), book.getAuthor().getName()),
            book.getCategories().stream().sorted(Comparator.comparing(Category::getId))
                .map(c -> new NamedResponse(c.getId(), c.getName())).toList());
    }
}
