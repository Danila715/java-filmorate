package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FilmControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getUrl() {
        return "http://localhost:" + port + "/films";
    }

    @Test
    void shouldNotAllowEmptyName() {
        Film film = new Film();
        film.setName("");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowDescriptionOver200Chars() {
        Film film = new Film();
        film.setName("Valid");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowReleaseBefore1895() {
        Film film = new Film();
        film.setName("Old");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(120);

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldNotAllowNonPositiveDuration() {
        Film film = new Film();
        film.setName("Short");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        ResponseEntity<String> response = restTemplate.postForEntity(getUrl(), film, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldAllowValidFilm() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Good movie");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        film.setMpa(mpa);

        ResponseEntity<Film> response = restTemplate.postForEntity(getUrl(), film, Film.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getId() > 0);
        assertEquals("Valid Film", response.getBody().getName());
    }
}