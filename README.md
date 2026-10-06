# Библиотека

[![Build](https://github.com/AbdulGus/-547/actions/workflows/build.yml/badge.svg)](https://github.com/AbdulGus/-547/actions/workflows/build.yml)

Учебный проект: каталог библиотеки на Spring Boot. Пользователь может зарегистрироваться, найти книгу и посмотреть автора и категории. Администратор добавляет, изменяет и удаляет записи через тот же интерфейс.

## Стек

Java 21, Spring Boot 4.0.8, Spring Web MVC, Spring Data JPA, Spring Security, PostgreSQL 17, Flyway, JWT, springdoc-openapi 3.0.1. Интерфейс — HTML, CSS и JavaScript без отдельного сервера и сборки. Тесты — JUnit 5, Mockito, MockMvc, H2.

## Запуск через Docker

Нужен Docker с Compose.

```bash
git clone https://github.com/AbdulGus/-547.git
cd ./-547
cp .env.example .env
docker compose up --build
```

В PowerShell вместо `cp` можно использовать `Copy-Item .env.example .env`.

Перед запуском задайте свои значения в `.env`. Для локальной проверки подойдут значения из примера. Для JWT нужен случайный секрет длиной не менее 32 байт; удобнее использовать 64 латинских символа. Файл `.env` не попадает в Git.

| Адрес | Что находится |
| --- | --- |
| http://localhost:8080 | Веб-интерфейс |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

Первый запуск создаёт таблицы и добавляет три книги. Если оставлены значения из `.env.example`, администратор: `admin@library.local`, пароль `LibraryAdmin123!`. Администратор создаётся при запуске только при отсутствии такого email. Повторный запуск не меняет пароль и роль существующего пользователя. Если email уже занят читателем, выберите другой ADMIN_EMAIL.

```bash
docker compose logs -f app
docker compose down
```

Обычная остановка сохраняет базу в volume `library_data`. Команда `docker compose down -v` удаляет и базу.

## Локальный запуск

Нужны JDK 21 или новее, Maven 3.9+ и PostgreSQL 17. Можно запустить только базу из Compose:

```bash
docker compose up -d db
```

Либо создать базу в установленном PostgreSQL:

```sql
CREATE USER library WITH PASSWORD 'library-local-password';
CREATE DATABASE library OWNER library;
```

Переменные окружения для Bash:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/library
export DB_USER=library
export DB_PASSWORD=library-local-password
export JWT_SECRET=local-library-key-replace-with-random-64-character-secret-before-deploy
export ADMIN_EMAIL=admin@library.local
export ADMIN_PASSWORD='LibraryAdmin123!'
mvn spring-boot:run
```

Для PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/library'
$env:DB_USER = 'library'
$env:DB_PASSWORD = 'library-local-password'
$env:JWT_SECRET = 'local-library-key-replace-with-random-64-character-secret-before-deploy'
$env:ADMIN_EMAIL = 'admin@library.local'
$env:ADMIN_PASSWORD = 'LibraryAdmin123!'
mvn spring-boot:run
```

При локальном запуске Spring не читает `.env` автоматически: переменные нужно задать в терминале или настройках запуска IDE. ADMIN_EMAIL и ADMIN_PASSWORD можно не указывать, если создание администратора не нужно. DB_PASSWORD и JWT_SECRET обязательны.

Сборка исполняемого JAR:

```bash
mvn clean verify
java -jar target/library-1.0.0.jar
```

## Как пользоваться

1. Открыть http://localhost:8080.
2. Зарегистрировать читателя или войти администратором.
3. На вкладке «Книги» доступен поиск по названию и переключение страниц.
4. Администратору доступна кнопка «Добавить» и действия на карточках.
5. Для новой книги сначала создать автора и категорию, затем заполнить форму книги.

Токен хранится в localStorage под ключом `library-token`. При выходе он удаляется. Токен действует один час; после истечения приложение просит войти снова. Выход из браузера не отзывает уже выданный JWT на сервере. Данные выводятся через textContent, без вставки пользовательского HTML.

## Авторизация и Swagger

Все методы каталога и `GET /api/auth/me` требуют JWT. Исключения — `POST /api/auth/register`, `POST /api/auth/login`, файлы страницы входа и Swagger. Это точки входа для получения токена и загрузки интерфейса; без исключения для входа пользователь не смог бы получить JWT. Анонимного чтения или изменения каталога нет.

Регистрация всегда создаёт USER. Передать себе ADMIN в JSON нельзя. Пароли хранятся как BCrypt-хеши. JWT проверяется по подписи, сроку и issuer; актуальная роль берётся из базы.

В Swagger открыть раздел «Авторизация», выполнить `POST /api/auth/login`:

```json
{
  "email": "admin@library.local",
  "password": "LibraryAdmin123!"
}
```

Скопировать поле `token` из ответа. Нажать **Authorize**, вставить сам токен без слова Bearer и подтвердить. Затем можно вызывать защищённые операции. У операций есть теги, модели request/response и схемы ошибок.

Пример через curl:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@library.local","password":"LibraryAdmin123!"}'

curl "http://localhost:8080/api/books?page=0&size=10&search=" \
  -H "Authorization: Bearer ВАШ_ТОКЕН"
```

В Windows при необходимости использовать `curl.exe`.

## API

| Метод | Путь | Доступ |
| --- | --- | --- |
| POST | /api/auth/register | Регистрация |
| POST | /api/auth/login | Получение токена |
| GET | /api/auth/me | USER, ADMIN |
| GET | /api/books, /api/books/{id} | USER, ADMIN |
| POST | /api/books | ADMIN |
| PUT, DELETE | /api/books/{id} | ADMIN |
| GET | /api/authors, /api/authors/{id} | USER, ADMIN |
| POST | /api/authors | ADMIN |
| PUT, DELETE | /api/authors/{id} | ADMIN |
| GET | /api/categories, /api/categories/{id} | USER, ADMIN |
| POST | /api/categories | ADMIN |
| PUT, DELETE | /api/categories/{id} | ADMIN |

Все списки принимают `page` (с нуля) и `size` (1–100). Список книг дополнительно принимает `search`. Книги сортируются по ID по убыванию, справочники — по возрастанию. Ответ списка: `content`, `page`, `size`, `totalElements`, `totalPages`.

Пример создания книги, если существуют автор 1 и категория 1:

```json
{
  "title": "Капитанская дочка",
  "isbn": "9785170906314",
  "publicationYear": 1836,
  "authorId": 1,
  "categoryIds": [1]
}
```

Автор и категория создаются JSON-объектом с полем `name`. POST возвращает 201 и Location, PUT — 200, DELETE — 204.

## Архитектура и правила

```text
controller → service → repository → PostgreSQL
      ↕
request / response DTO
```

- `controller` — HTTP, валидация, DTO и аннотации доступа.
- `service` — бизнес-правила, транзакции и преобразование в DTO.
- `repository` — JPA, Query Methods, @Query и @EntityGraph.
- `entity` — модели базы.
- `security` — JWT, фильтр и конфигурация прав.
- `exception` — единый ErrorResponse и @RestControllerAdvice.
- `config` — OpenAPI и создание администратора.
- `resources/static` — интерфейс.

Связи: Author → Book — OneToMany, Book → Author — ManyToOne, Book ↔ Category — ManyToMany через book_categories. AppUser хранит учётные записи. JPA Entity не принимаются и не возвращаются контроллерами. Open Session in View выключен.

Бизнес-правила:

1. Только ADMIN меняет каталог. Проверки @PreAuthorize выполняются на сервере.
2. Нельзя удалить автора или категорию, пока существуют связанные книги. Возвращается 409.
3. ISBN уникален и состоит из 13 цифр. Год издания — от 1450 до текущего года.
4. У книги должен быть существующий автор и от 1 до 10 существующих категорий.
5. Название категории проверяется на повтор без учёта регистра; email приводится к нижнему регистру.

Уникальность ISBN/email и внешние ключи дополнительно обеспечиваются базой данных.

Ошибки валидации, бизнес-ошибки и ошибки безопасности проходят через общий обработчик. Фильтр и обработчики Spring Security передают исключения в HandlerExceptionResolver.

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 400,
  "message": "Проверьте введённые данные",
  "path": "/api/books",
  "errors": {
    "isbn": "ISBN должен содержать 13 цифр"
  }
}
```

Основные коды: 400 — неверные данные, 401 — нет действующего токена, 403 — не хватает прав, 404 — запись не найдена, 409 — конфликт данных.

## N+1

При обычной выборке книг LAZY-связи загружаются при обращении к автору и категориям. Для 10 книг с разными авторами получается 1 запрос книг + 10 запросов авторов + 10 запросов категорий = 21 запрос, без учёта COUNT.

В BookRepository сначала выбирается страница ID и общее количество, затем один запрос с @EntityGraph загружает книги вместе с авторами и категориями. Порядок восстанавливается по странице ID. Ограничение страницы применяется к ID, поэтому JOIN коллекции не приводит к пагинации в памяти.

| Сценарий | Число SQL |
| --- | --- |
| Обычная выборка 10 книг и обход связей | 21 без COUNT / 22 с COUNT |
| Выборка через BookService | 3, включая COUNT |

Это проверяет QueryCountTest на 12 книгах с отдельными авторами. Он очищает persistence context перед каждым измерением и использует Hibernate Statistics:

```text
N+1: before=21 (without count), after=3 (with count), books=10
```

В HTTP-запросе добавляется один запрос пользователя для проверки JWT. Для неполной последней страницы Spring Data может не выполнять COUNT.

Чтобы увидеть SQL:

```bash
mvn test -Dtest=QueryCountTest -Dspring.jpa.show-sql=true
```

В PowerShell параметр с точками нужно заключить в кавычки: `'-Dspring.jpa.show-sql=true'`.

## Миграции

Hibernate только проверяет схему (`ddl-auto=validate`). Создание таблиц и начальных данных выполняет Flyway:

1. V1 — пользователи.
2. V2 — авторы.
3. V3 — категории.
4. V4 — книги и индекс автора.
5. V5 — связь книг и категорий.
6. V6 — начальный каталог.

Скрипты находятся в `src/main/resources/db/migration`. После применения миграции не редактируются — изменения схемы добавляются новым файлом.

## Тесты

```bash
mvn clean verify
```

- BookServiceTest: будущий год, повтор ISBN, отсутствующая категория.
- AuthorServiceTest: запрет удаления используемого автора и удаление свободного.
- ApiIntegrationTest: настоящие подписанные JWT, 401/403, регистрация, вход, запрет повышения роли, валидация, полный CRUD и Swagger.
- QueryCountTest: сравнение числа запросов до и после оптимизации.

Для обычного запуска тестов используется H2 в режиме PostgreSQL; внешняя база и Docker не нужны. Flyway применяет те же миграции, что и при запуске приложения. В GitHub Actions отдельное задание запускает интеграционные тесты с PostgreSQL 17. Dockerfile собирает приложение с запуском тестов.
