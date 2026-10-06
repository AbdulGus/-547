package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
