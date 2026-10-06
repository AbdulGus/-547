package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
