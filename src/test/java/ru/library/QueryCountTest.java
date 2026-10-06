package ru.library;

import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.library.entity.*;
import ru.library.service.BookService;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QueryCountTest {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired BookService service;

    @Test
    void paginatedCatalogHasConstantQueryCount() {
        Category category = new Category();
        category.setName("N+1 test");
        em.persist(category);
        for (int i = 0; i < 12; i++) {
            Author author = new Author();
            author.setName("Author " + i);
            em.persist(author);
            Book book = new Book();
            book.setTitle("N+1 book " + i);
            book.setIsbn("99900000000" + String.format("%02d", i));
            book.setPublicationYear(2020);
            book.setAuthor(author);
            book.setCategories(Set.of(category));
            em.persist(book);
        }
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var naive = em.createQuery("select b from Book b where b.title like 'N+1 book%' order by b.id", Book.class)
            .setMaxResults(10).getResultList();
        naive.forEach(b -> {
            b.getAuthor().getName();
            b.getCategories().size();
        });
        long before = stats.getPrepareStatementCount();
        assertEquals(21, before);
        em.clear();
        stats.clear();
        var page = service.list("N+1 book", 0, 10);
        assertEquals(10, page.content().size());
        assertEquals(12, page.totalElements());
        assertTrue(page.content().stream().allMatch(b -> !b.categories().isEmpty()));
        long after = stats.getPrepareStatementCount();
        assertEquals(3, after);
        System.out.println("N+1: before=" + before + " (without count), after=" + after + " (with count), books=10");
    }
}
