package ru.library.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.dto.*;
import ru.library.entity.Author;
import ru.library.exception.ApiException;
import ru.library.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorService {
    private final AuthorRepository repository;
    private final BookRepository books;

    public PageResponse<NamedResponse> list(int page, int size) {
        return PageResponse.from(repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(this::response));
    }

    public NamedResponse get(Long id) {
        return response(find(id));
    }

    @Transactional
    public NamedResponse create(AuthorRequest request) {
        Author item = new Author();
        apply(item, request);
        return response(repository.save(item));
    }

    @Transactional
    public NamedResponse update(Long id, AuthorRequest request) {
        Author item = find(id);
        apply(item, request);
        return response(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        Author item = find(id);
        if (books.existsByAuthorId(id)) {
            throw ApiException.conflict("Нельзя удалить: есть связанные книги");
        }
        repository.delete(item);
    }

    private void apply(Author item, AuthorRequest request) {
        item.setName(request.name().trim());
    }

    private Author find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Автор"));
    }

    private NamedResponse response(Author item) {
        return new NamedResponse(item.getId(), item.getName());
    }
}
