package ru.library;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
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
            .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists());
    }

    long create(String path, Object body) throws Exception {
        String response = mvc.perform(post(path).header("Authorization", "Bearer " + admin)
                .contentType("application/json").content(mapper.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }
}
