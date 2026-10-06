package ru.library.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.dto.*;
import ru.library.entity.Category;
import ru.library.exception.ApiException;
import ru.library.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    private final BookRepository books;

    public PageResponse<NamedResponse> list(int page, int size) {
        return PageResponse.from(repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(this::response));
    }

    public NamedResponse get(Long id) {
        return response(find(id));
    }

    @Transactional
    public NamedResponse create(CategoryRequest request) {
        Category item = new Category();
        apply(item, request);
        return response(repository.save(item));
    }

    @Transactional
    public NamedResponse update(Long id, CategoryRequest request) {
        Category item = find(id);
        apply(item, request);
        return response(repository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        Category item = find(id);
        if (books.existsByCategoriesId(id)) {
            throw ApiException.conflict("Нельзя удалить: есть связанные книги");
        }
        repository.delete(item);
    }

    private void apply(Category item, CategoryRequest request) {
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), item.getId() == null ? 0L : item.getId())) {
            throw ApiException.conflict("Категория уже существует");
        }
        item.setName(request.name().trim());
    }

    private Category find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Категория"));
    }

    private NamedResponse response(Category item) {
        return new NamedResponse(item.getId(), item.getName());
    }
}
