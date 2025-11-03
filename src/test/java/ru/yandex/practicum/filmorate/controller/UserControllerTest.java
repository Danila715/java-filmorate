package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getUrl() {
        return "http://localhost:" + port + "/users";
    }

    @Test
    void shouldNotAllowEmptyEmail() {
        User user = new User();
        user.setEmail("");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowEmailWithoutAtSymbol() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowEmptyLogin() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowLoginWithSpaces() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("invalid login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowBirthdayInFuture() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.now().plusDays(1));

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), user, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldUseLoginAsNameIfNameIsEmpty() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("cooluser");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<User> response = restTemplate.postForEntity(getUrl(), user, User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cooluser", response.getBody().getName());
    }

    @Test
    void shouldCreateValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("validLogin");
        user.setName("John Doe");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ResponseEntity<User> response = restTemplate.postForEntity(getUrl(), user, User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("John Doe", response.getBody().getName());
    }

    @Test
    void shouldUpdateUserPartially() {
        // Создаём
        User user = new User();
        user.setEmail("old@example.com");
        user.setLogin("oldLogin");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1980, 1, 1));
        ResponseEntity<User> created = restTemplate.postForEntity(getUrl(), user, User.class);
        User createdUser = created.getBody();

        // Обновляем
        User update = new User();
        update.setId(createdUser.getId());
        update.setEmail("new@example.com");
        update.setName("New Name");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<User> request = new HttpEntity<>(update, headers);

        ResponseEntity<User> updated = restTemplate.exchange(getUrl(), HttpMethod.PUT, request, User.class);
        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals("new@example.com", updated.getBody().getEmail());
        assertEquals("oldLogin", updated.getBody().getLogin());
        assertEquals("New Name", updated.getBody().getName());
    }

    @Test
    void shouldNotUpdateNonExistentUser() {
        User user = new User();
        user.setId(999);
        user.setEmail("test@example.com");
        user.setLogin("test");
        user.setName("Test");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<User> request = new HttpEntity<>(user, headers);

        ResponseEntity<String> response = restTemplate.exchange(getUrl(), HttpMethod.PUT, request, String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}