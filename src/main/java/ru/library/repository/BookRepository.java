package ru.library.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.library.entity.Book;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "select b.id from Book b where lower(b.title) like lower(concat('%', :search, '%'))",
        countQuery = "select count(b) from Book b where lower(b.title) like lower(concat('%', :search, '%'))")
    Page<Long> findPageIds(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "categories"})
    @Query("select distinct b from Book b where b.id in :ids")
    List<Book> findWithRelationsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"author", "categories"})
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findWithRelationsById(@Param("id") Long id);

    boolean existsByIsbnAndIdNot(String isbn, Long id);
    boolean existsByAuthorId(Long id);
    boolean existsByCategoriesId(Long id);
}
