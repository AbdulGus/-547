package ru.library;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.library.entity.*;
import ru.library.repository.UserRepository;
import ru.library.security.JwtService;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Value("${app.jwt.secret}") String secret;
    String admin;
    String reader;

    @BeforeEach
    void setup() {
        admin = createUser("admin@test.local", Role.ADMIN);
        reader = createUser("reader@test.local", Role.USER);
    }

    String createUser(String email, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(encoder.encode("Password123"));
        user.setRole(role);
        users.saveAndFlush(user);
        return jwt.issue(email);
    }

    @Test
    void booksRequireJwt() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.path").value("/api/books"));
        mvc.perform(get("/api/books").header("Authorization", "Bearer " + reader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].author.name").isString())
            .andExpect(jsonPath("$.content[0].categories").isArray());
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        mvc.perform(get("/api/books").header("Authorization", "Bearer invalid.token.value"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void readerCannotModifyCatalog() throws Exception {
        mvc.perform(post("/api/authors").header("Authorization", "Bearer " + reader)
                .contentType("application/json").content("{\"name\":\"Новый автор\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void registrationAndLoginReturnUsableJwt() throws Exception {
        String body = "{\"email\":\"New@Test.Local\",\"password\":\"Password123\",\"role\":\"ADMIN\"}";
        String registered = mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.email").value("new@test.local")).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(registered).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"new@test.local\",\"password\":\"Password123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"new@test.local\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesBodyAndPagination() throws Exception {
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists());
        mvc.perform(get("/api/books?size=0").header("Authorization", "Bearer " + reader))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books?page=-1").header("Authorization", "Bearer " + reader))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books/99999").header("Authorization", "Bearer " + reader))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCanManageEntireCatalog() throws Exception {
        long authorId = create("/api/authors", Map.of("name", "Тестовый автор"));
        long categoryId = create("/api/categories", Map.of("name", "Тестовая категория"));
        long bookId = create("/api/books", Map.of("title", "Тестовая книга", "isbn", "9785170000001",
            "publicationYear", 2020, "authorId", authorId, "categoryIds", new long[]{categoryId}));
        mvc.perform(delete("/api/authors/" + authorId).header("Authorization", "Bearer " + admin))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/categories/" + categoryId).header("Authorization", "Bearer " + admin))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/books/" + bookId).header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(Map.of(
                    "title", "Изменённая книга", "isbn", "9785170000001", "publicationYear", 2021,
                    "authorId", authorId, "categoryIds", new long[]{categoryId}))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Изменённая книга"));
        mvc.perform(get("/api/books?search=Изменённая&size=1").header("Authorization", "Bearer " + reader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put("/api/authors/" + authorId).header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{\"name\":\"Другое имя\"}"))
            .andExpect(status().isOk());
        mvc.perform(put("/api/categories/" + categoryId).header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{\"name\":\"Другое название\"}"))
            .andExpect(status().isOk());
        for (String path : new String[]{"/api/books/" + bookId, "/api/authors/" + authorId, "/api/categories/" + categoryId}) {
            mvc.perform(delete(path).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
            mvc.perform(get(path).header("Authorization", "Bearer " + reader)).andExpect(status().isNotFound());
        }
    }

    @Test
    void swaggerDescribesBearerAndDtos() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.components.schemas.BookRequest").exists())
            .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
            .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
            .andExpect(jsonPath("$.paths['/api/books'].get.tags[0]").value("Книги"))
            .andExpect(jsonPath("$.paths['/api/books'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/books/{id}'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['/api/books'].post.responses['415']").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"books", "authors", "categories"})
    void checksAccessForEveryCatalogOperation(String resource) throws Exception {
        String base = "/api/" + resource;
        Object payload = resource.equals("books")
            ? Map.of("title", "Книга", "isbn", "9785170000002", "publicationYear", 2020,
                "authorId", 1, "categoryIds", new int[]{1})
            : Map.of("name", "Новая запись");
        String body = mapper.writeValueAsString(payload);
        for (String path : new String[]{base, base + "/1"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
            mvc.perform(get(path).header("Authorization", "Bearer " + reader)).andExpect(status().isOk());
        }
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
            String path = method == HttpMethod.POST ? base : base + "/1";
            mvc.perform(request(method, path).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
            mvc.perform(request(method, path).contentType("application/json").content(body)
                    .header("Authorization", "Bearer " + reader))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    void rejectsExpiredAndWronglySignedTokens() throws Exception {
        String expired = new JwtService(secret, -60).issue("reader@test.local");
        String wrongSignature = new JwtService("another-secret-key-that-is-at-least-32-bytes", 3600)
            .issue("reader@test.local");
        for (String token : new String[]{expired, wrongSignature, jwt.issue("deleted@test.local")}) {
            mvc.perform(get("/api/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void usesCurrentRoleFromDatabase() throws Exception {
        AppUser user = users.findByEmail("admin@test.local").orElseThrow();
        user.setRole(Role.USER);
        users.saveAndFlush(user);
        mvc.perform(post("/api/authors").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{\"name\":\"Автор\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void storesPasswordAsHashAndNeverReturnsIt() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"email\":\"hash@test.local\",\"password\":\"Password123\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        AppUser user = users.findByEmail("hash@test.local").orElseThrow();
        assertNotEquals("Password123", user.getPassword());
        assertTrue(encoder.matches("Password123", user.getPassword()));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.issue(user.getEmail())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void returnsConsistentErrorsForHttpFailures() throws Exception {
        mvc.perform(post("/api/authors").header("Authorization", "Bearer " + admin)
                .contentType("text/plain").content("name=Author"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.path").value("/api/authors")).andExpect(jsonPath("$.errors").isMap());
        mvc.perform(get("/api/books").header("Authorization", "Bearer " + reader).accept("text/plain"))
            .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.status").value(406));
        mvc.perform(patch("/api/books/1").header("Authorization", "Bearer " + admin))
            .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("email", "admin@test.local", "password", "я".repeat(64)))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void validatesBookRelationsAndBusinessRules() throws Exception {
        var body = new java.util.HashMap<String, Object>(Map.of("title", "Книга", "isbn", "9785170000003",
            "publicationYear", 2020, "authorId", 1, "categoryIds", new int[]{1}));
        body.put("publicationYear", Year.now().getValue() + 1);
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
        body.put("publicationYear", 2020);
        body.put("authorId", 99999);
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());
        body.put("authorId", 1);
        body.put("categoryIds", new int[]{99999});
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());
        body.put("categoryIds", new int[]{});
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
        body.put("categoryIds", new int[]{1});
        body.put("isbn", "9785170906307");
        mvc.perform(post("/api/books").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + admin)
                .contentType("application/json").content("{\"name\":\"  классика  \"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void paginationHasNoDuplicatesOrMissingBooks() throws Exception {
        String first = mvc.perform(get("/api/books?size=2&page=0").header("Authorization", "Bearer " + reader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2))
            .andReturn().getResponse().getContentAsString();
        String second = mvc.perform(get("/api/books?size=2&page=1").header("Authorization", "Bearer " + reader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
            .andReturn().getResponse().getContentAsString();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (String response : new String[]{first, second}) {
            for (var book : mapper.readTree(response).get("content")) {
                assertTrue(ids.add(book.get("id").asLong()));
                assertFalse(book.get("categories").isEmpty());
            }
        }
        assertEquals(3, ids.size());
        mvc.perform(get("/api/books?size=101").header("Authorization", "Bearer " + reader))
            .andExpect(status().isBadRequest());
    }

    long create(String path, Object body) throws Exception {
        String response = mvc.perform(post(path).header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }
}
