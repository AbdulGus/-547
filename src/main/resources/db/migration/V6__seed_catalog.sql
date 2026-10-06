INSERT INTO authors(name) VALUES ('Александр Пушкин'), ('Михаил Булгаков'), ('Антон Чехов');
INSERT INTO categories(name) VALUES ('Классика'), ('Роман'), ('Рассказы');
INSERT INTO books(title, isbn, publication_year, author_id)
SELECT 'Евгений Онегин', '9785170906307', 1833, id FROM authors WHERE name = 'Александр Пушкин';
INSERT INTO books(title, isbn, publication_year, author_id)
SELECT 'Мастер и Маргарита', '9785170904655', 1967, id FROM authors WHERE name = 'Михаил Булгаков';
INSERT INTO books(title, isbn, publication_year, author_id)
SELECT 'Рассказы', '9785389066984', 1900, id FROM authors WHERE name = 'Антон Чехов';
INSERT INTO book_categories(book_id, category_id)
SELECT b.id, c.id FROM books b CROSS JOIN categories c
WHERE c.name = 'Классика' OR (c.name = 'Роман' AND b.title <> 'Рассказы') OR (c.name = 'Рассказы' AND b.title = 'Рассказы');
