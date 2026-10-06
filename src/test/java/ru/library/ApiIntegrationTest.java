package ru.library;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import ru.library.entity.AppUser;
import ru.library.repository.UserRepository;
import ru.library.security.JwtService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;

    @Test
    void booksRequireJwt() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("reader@test.local");
        user.setPassword(encoder.encode("Password123"));
        users.saveAndFlush(user);
        String token = jwt.issue(user.getEmail());

        mvc.perform(get("/api/books"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/books").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3));
    }
}
